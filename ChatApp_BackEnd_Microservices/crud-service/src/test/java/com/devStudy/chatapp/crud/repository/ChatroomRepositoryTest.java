package com.devStudy.chatapp.crud.repository;

import com.devStudy.chatapp.crud.model.Chatroom;
import com.devStudy.chatapp.crud.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ChatroomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatroomRepository chatroomRepository;

    private User creator;
    private User member1;
    private User member2;
    private Chatroom activeChatroom;
    private Chatroom expiredChatroom;
    private Chatroom futureChatroom;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        // 创建测试用户
        creator = new User();
        creator.setFirstName("Creator");
        creator.setLastName("User");
        creator.setMail("creator@test.com");
        creator.setAdmin(false);
        creator.setActive(true);

        member1 = new User();
        member1.setFirstName("Member");
        member1.setLastName("One");
        member1.setMail("member1@test.com");
        member1.setAdmin(false);
        member1.setActive(true);

        member2 = new User();
        member2.setFirstName("Member");
        member2.setLastName("Two");
        member2.setMail("member2@test.com");
        member2.setAdmin(false);
        member2.setActive(true);

        // 持久化用户
        creator = entityManager.persistAndFlush(creator);
        member1 = entityManager.persistAndFlush(member1);
        member2 = entityManager.persistAndFlush(member2);

        // 创建活跃聊天室
        activeChatroom = new Chatroom();
        activeChatroom.setTitre("Active Chatroom");
        activeChatroom.setDescription("Active Description");
        activeChatroom.setCreator(creator);
        activeChatroom.setHoraireCommence(LocalDateTime.now().minusHours(1));
        activeChatroom.setHoraireTermine(LocalDateTime.now().plusHours(1));
        activeChatroom.setActive(true);
        activeChatroom = entityManager.persistAndFlush(activeChatroom);

        // 创建已过期聊天室
        expiredChatroom = new Chatroom();
        expiredChatroom.setTitre("Expired Chatroom");
        expiredChatroom.setDescription("Expired Description");
        expiredChatroom.setCreator(creator);
        expiredChatroom.setHoraireCommence(LocalDateTime.now().minusDays(2));
        expiredChatroom.setHoraireTermine(LocalDateTime.now().minusDays(1));
        expiredChatroom.setActive(true);
        expiredChatroom = entityManager.persistAndFlush(expiredChatroom);

        // 创建未来开始的聊天室
        futureChatroom = new Chatroom();
        futureChatroom.setTitre("Future Chatroom");
        futureChatroom.setDescription("Future Description");
        futureChatroom.setCreator(creator);
        futureChatroom.setHoraireCommence(LocalDateTime.now().plusDays(1));
        futureChatroom.setHoraireTermine(LocalDateTime.now().plusDays(2));
        futureChatroom.setActive(true);
        futureChatroom = entityManager.persistAndFlush(futureChatroom);

        // 添加成员到活跃聊天室
        member1.getJoinedRooms().add(activeChatroom);
        member2.getJoinedRooms().add(activeChatroom);
        activeChatroom.getMembers().add(member1);
        activeChatroom.getMembers().add(member2);
        entityManager.persistAndFlush(member1);
        entityManager.persistAndFlush(member2);

        // 添加成员到未来聊天室
        member1.getJoinedRooms().add(futureChatroom);
        futureChatroom.getMembers().add(member1);
        entityManager.persistAndFlush(member1);

        entityManager.clear();
    }

    @Test
    void testFindById() {
        // 测试按ID查找聊天室
        Optional<Chatroom> result = chatroomRepository.findById(activeChatroom.getId());
        
        assertThat(result).isPresent();
        assertThat(result.get().getTitre()).isEqualTo("Active Chatroom");
        assertThat(result.get().getCreator().getId()).isEqualTo(creator.getId());
    }

    @Test
    void testFindById_NotFound() {
        // 测试查找不存在的聊天室
        Optional<Chatroom> result = chatroomRepository.findById(99999L);
        
        assertThat(result).isEmpty();
    }

    @Test
    void testUpdateActive() {
        // 测试更新聊天室活跃状态
        chatroomRepository.updateActive(activeChatroom.getId(), false);
        entityManager.flush();
        entityManager.clear();

        Optional<Chatroom> result = chatroomRepository.findById(activeChatroom.getId());
        assertThat(result).isPresent();
        assertThat(result.get().isActive()).isFalse();
    }

    @Test
    void testFindChatroomsJoinedOfUserByPage() {
        // 测试查找用户加入的未过期聊天室
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Chatroom> result = chatroomRepository.findChatroomsJoinedOfUserByPage(member1.getId(), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Chatroom::getTitre)
                .containsExactlyInAnyOrder("Active Chatroom", "Future Chatroom");
        
        // 验证都是未过期的聊天室
        assertThat(result.getContent()).allSatisfy(chatroom -> 
                assertThat(chatroom.getHoraireTermine()).isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void testFindChatroomsJoinedOfUserByPage_OnlyActive() {
        // member2只加入了活跃聊天室
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Chatroom> result = chatroomRepository.findChatroomsJoinedOfUserByPage(member2.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitre()).isEqualTo("Active Chatroom");
    }

    @Test
    void testFindChatroomsCreatedByUserByPage() {
        // 测试查找用户创建的未过期聊天室
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Chatroom> result = chatroomRepository.findChatroomsCreatedByUserByPage(creator.getId(), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Chatroom::getTitre)
                .containsExactlyInAnyOrder("Active Chatroom", "Future Chatroom");
        
        // 验证都是未过期的聊天室
        assertThat(result.getContent()).allSatisfy(chatroom -> 
                assertThat(chatroom.getHoraireTermine()).isAfter(LocalDateTime.now().minusMinutes(1)));
        
        // 验证创建者正确
        assertThat(result.getContent()).allSatisfy(chatroom -> 
                assertThat(chatroom.getCreator().getId()).isEqualTo(creator.getId()));
    }

    @Test
    void testFindChatroomsCreatedByUserByPage_NoResults() {
        // 测试用户没有创建任何聊天室的情况
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Chatroom> result = chatroomRepository.findChatroomsCreatedByUserByPage(member1.getId(), pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void testFindByIdAndCreatorId() {
        // 测试按ID和创建者ID查找聊天室
        Optional<Chatroom> result = chatroomRepository.findByIdAndCreatorId(
                activeChatroom.getId(), creator.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTitre()).isEqualTo("Active Chatroom");
        assertThat(result.get().getCreator().getId()).isEqualTo(creator.getId());
    }

    @Test
    void testFindByIdAndCreatorId_WrongCreator() {
        // 测试用错误的创建者ID查找聊天室
        Optional<Chatroom> result = chatroomRepository.findByIdAndCreatorId(
                activeChatroom.getId(), member1.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void testFindByIdAndCreatorId_NotFound() {
        // 测试查找不存在的聊天室
        Optional<Chatroom> result = chatroomRepository.findByIdAndCreatorId(
                99999L, creator.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void testPagination() {
        // 创建更多聊天室以测试分页
        for (int i = 0; i < 5; i++) {
            Chatroom chatroom = new Chatroom();
            chatroom.setTitre("Test Chatroom " + i);
            chatroom.setDescription("Test Description " + i);
            chatroom.setCreator(creator);
            chatroom.setHoraireCommence(LocalDateTime.now());
            chatroom.setHoraireTermine(LocalDateTime.now().plusDays(1));
            chatroom.setActive(true);
            entityManager.persistAndFlush(chatroom);
        }

        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<Chatroom> firstPage = chatroomRepository.findChatroomsCreatedByUserByPage(
                creator.getId(), pageRequest);

        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(7); // 2个原有的 + 5个新创建的
        assertThat(firstPage.hasNext()).isTrue();

        PageRequest pageRequest2 = PageRequest.of(1, 3);
        Page<Chatroom> secondPage = chatroomRepository.findChatroomsCreatedByUserByPage(
                creator.getId(), pageRequest2);

        assertThat(secondPage.getContent()).hasSize(3);
        assertThat(secondPage.hasNext()).isTrue();
    }
}