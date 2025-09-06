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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User normalUser1;
    private User normalUser2;
    private User normalUser3;
    private Chatroom chatroom;

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
        adminUser = new User();
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setMail("admin@test.com");
        adminUser.setAdmin(true);
        adminUser.setActive(true);

        normalUser1 = new User();
        normalUser1.setFirstName("Normal");
        normalUser1.setLastName("User1");
        normalUser1.setMail("user1@test.com");
        normalUser1.setAdmin(false);
        normalUser1.setActive(true);

        normalUser2 = new User();
        normalUser2.setFirstName("Normal");
        normalUser2.setLastName("User2");
        normalUser2.setMail("user2@test.com");
        normalUser2.setAdmin(false);
        normalUser2.setActive(true);

        normalUser3 = new User();
        normalUser3.setFirstName("Normal");
        normalUser3.setLastName("User3");
        normalUser3.setMail("user3@test.com");
        normalUser3.setAdmin(false);
        normalUser3.setActive(true);

        // 持久化用户
        adminUser = entityManager.persistAndFlush(adminUser);
        normalUser1 = entityManager.persistAndFlush(normalUser1);
        normalUser2 = entityManager.persistAndFlush(normalUser2);
        normalUser3 = entityManager.persistAndFlush(normalUser3);

        // 创建聊天室
        chatroom = new Chatroom();
        chatroom.setTitre("Test Chatroom");
        chatroom.setDescription("Test Description");
        chatroom.setCreator(normalUser1);
        chatroom.setHoraireCommence(LocalDateTime.now());
        chatroom.setHoraireTermine(LocalDateTime.now().plusDays(1));
        chatroom.setActive(true);

        chatroom = entityManager.persistAndFlush(chatroom);

        // 添加成员到聊天室
        normalUser2.getJoinedRooms().add(chatroom);
        chatroom.getMembers().add(normalUser2);
        entityManager.persistAndFlush(normalUser2);

        entityManager.clear();
    }

    @Test
    void testFindAllOtherUsersNotAdminByPage() {
        // 测试查找除指定用户外的所有非管理员用户
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findAllOtherUsersNotAdminByPage(normalUser1.getId(), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(User::getId)
                .containsExactlyInAnyOrder(normalUser2.getId(), normalUser3.getId());
        
        // 验证不包含管理员用户和自己
        assertThat(result.getContent()).extracting(User::getId)
                .doesNotContain(adminUser.getId(), normalUser1.getId());
    }

    @Test
    void testFindUsersInvitedToChatroomByPage() {
        // 测试查找聊天室中的非管理员成员
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findUsersInvitedToChatroomByPage(chatroom.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(normalUser2.getId());
        
        // 验证不包含管理员用户
        assertThat(result.getContent()).extracting(User::isAdmin).containsOnly(false);
    }

    @Test
    void testFindUsersNotInvitedToChatroomByPage() {
        // 测试查找未加入聊天室的用户（排除创建者和管理员）
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findUsersNotInvitedToChatroomByPage(
                chatroom.getId(), normalUser1.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(normalUser3.getId());
        
        // 验证不包含创建者、管理员和已加入的用户
        assertThat(result.getContent()).extracting(User::getId)
                .doesNotContain(normalUser1.getId(), normalUser2.getId(), adminUser.getId());
    }

    @Test
    void testFindUsersNotInvitedToChatroomByPage_EmptyResult() {
        // 创建另一个聊天室，让所有用户都加入
        Chatroom fullChatroom = new Chatroom();
        fullChatroom.setTitre("Full Chatroom");
        fullChatroom.setDescription("Full Description");
        fullChatroom.setCreator(normalUser1);
        fullChatroom.setHoraireCommence(LocalDateTime.now());
        fullChatroom.setHoraireTermine(LocalDateTime.now().plusDays(1));
        fullChatroom.setActive(true);
        fullChatroom = entityManager.persistAndFlush(fullChatroom);

        // 添加所有普通用户到聊天室
        User user2 = entityManager.find(User.class, normalUser2.getId());
        User user3 = entityManager.find(User.class, normalUser3.getId());
        user2.getJoinedRooms().add(fullChatroom);
        user3.getJoinedRooms().add(fullChatroom);
        fullChatroom.getMembers().add(user2);
        fullChatroom.getMembers().add(user3);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findUsersNotInvitedToChatroomByPage(
                fullChatroom.getId(), normalUser1.getId(), pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void testFindUsersInvitedToChatroomByPage_EmptyResult() {
        // 创建一个没有成员的聊天室
        Chatroom emptyChatroom = new Chatroom();
        emptyChatroom.setTitre("Empty Chatroom");
        emptyChatroom.setDescription("Empty Description");
        emptyChatroom.setCreator(normalUser1);
        emptyChatroom.setHoraireCommence(LocalDateTime.now());
        emptyChatroom.setHoraireTermine(LocalDateTime.now().plusDays(1));
        emptyChatroom.setActive(true);
        emptyChatroom = entityManager.persistAndFlush(emptyChatroom);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findUsersInvitedToChatroomByPage(emptyChatroom.getId(), pageable);

        assertThat(result.getContent()).isEmpty();
    }
}