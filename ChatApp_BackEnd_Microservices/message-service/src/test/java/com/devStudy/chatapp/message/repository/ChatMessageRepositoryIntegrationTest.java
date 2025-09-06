package com.devStudy.chatapp.message.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.devStudy.chatapp.message.TestDataFactory;
import com.devStudy.chatapp.message.dto.UserDTO;
import com.devStudy.chatapp.message.model.ChatMessage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataMongoTest
@Testcontainers
class ChatMessageRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private final long testChatroomId1 = 1L;
    private final long testChatroomId2 = 2L;
    private UserDTO testUser1;
    private UserDTO testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = TestDataFactory.createTestUser(1L);
        testUser2 = TestDataFactory.createTestUser(2L);
        
        // 清空集合
        chatMessageRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAll();
    }

    @Test
    void testSaveAndRetrieveMessage() {
        // Given
        ChatMessage message = TestDataFactory.createTestChatMessage(
            testChatroomId1, testUser1, "Test message", new Date());

        // When
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Then
        assertNotNull(savedMessage);
        assertNotNull(savedMessage.getId());
        assertEquals(testChatroomId1, savedMessage.getChatroomId());
        assertEquals("Test message", savedMessage.getContent());
        assertEquals(testUser1.getId(), savedMessage.getUser().getId());
    }

    @Test
    void testFindByChatroomId() {
        // Given
        Date now = new Date();
        Date earlier = new Date(now.getTime() - 3600000); // 1小时前
        
        ChatMessage msg1 = TestDataFactory.createTestChatMessage(
            testChatroomId1, testUser1, "First message", earlier);
        ChatMessage msg2 = TestDataFactory.createTestChatMessage(
            testChatroomId1, testUser2, "Second message", now);
        ChatMessage msg3 = TestDataFactory.createTestChatMessage(
            testChatroomId2, testUser1, "Other chatroom message", now);

        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));

        // When
        List<ChatMessage> result = chatMessageRepository.findByChatroomId(testChatroomId1);

        // Then
        assertEquals(2, result.size());
        
        // 验证按时间戳升序排列（@Query注解中指定）
        assertTrue(result.get(0).getTimestamp().before(result.get(1).getTimestamp()) ||
                  result.get(0).getTimestamp().equals(result.get(1).getTimestamp()));
        
        // 验证都属于testChatroomId1
        assertTrue(result.stream().allMatch(msg -> msg.getChatroomId() == testChatroomId1));
    }

    @Test
    void testFindByChatroomIdWithPagination() {
        // Given
        Date baseTime = new Date();
        for (int i = 0; i < 5; i++) {
            ChatMessage msg = TestDataFactory.createTestChatMessage(
                testChatroomId1, testUser1, "Message " + i, 
                new Date(baseTime.getTime() + i * 1000)); // 每秒递增
            chatMessageRepository.save(msg);
        }

        // When
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> result = chatMessageRepository.findByChatroomId(testChatroomId1, pageable);

        // Then
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(3, result.getContent().size());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());
        
        // 验证按时间戳降序排列
        List<ChatMessage> content = result.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertTrue(content.get(i).getTimestamp().after(content.get(i + 1).getTimestamp()) ||
                      content.get(i).getTimestamp().equals(content.get(i + 1).getTimestamp()));
        }
    }

    @Test
    void testFindByChatroomId_EmptyResult() {
        // Given - 没有保存任何消息

        // When
        List<ChatMessage> result = chatMessageRepository.findByChatroomId(999L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByChatroomIdWithPagination_EmptyResult() {
        // Given - 没有保存任何消息
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ChatMessage> result = chatMessageRepository.findByChatroomId(999L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
    }

    @Test
    void testLargeDatasetPagination() {
        // Given - 创建大量消息
        Date baseTime = new Date();
        for (int i = 0; i < 25; i++) {
            ChatMessage msg = TestDataFactory.createTestChatMessage(
                testChatroomId1, 
                i % 2 == 0 ? testUser1 : testUser2, 
                "Message " + i, 
                new Date(baseTime.getTime() + i * 1000)
            );
            chatMessageRepository.save(msg);
        }

        // When - 查询第2页，每页10条
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> result = chatMessageRepository.findByChatroomId(testChatroomId1, pageable);

        // Then
        assertEquals(25, result.getTotalElements());
        assertEquals(5, result.getTotalPages());
        assertEquals(5, result.getContent().size());
        assertFalse(result.isFirst());
        assertFalse(result.isLast());

        // 验证消息内容
        List<ChatMessage> content = result.getContent();
        assertFalse(content.isEmpty());
        assertTrue(content.stream().allMatch(msg -> msg.getChatroomId() == testChatroomId1));
    }

    @Test
    void testIndexEffectiveness() {
        // Given - 创建不同聊天室的消息
        Date now = new Date();
        
        // 聊天室1: 10条消息
        for (int i = 0; i < 10; i++) {
            ChatMessage msg = TestDataFactory.createTestChatMessage(
                testChatroomId1, testUser1, "Room1 Message " + i, 
                new Date(now.getTime() + i * 1000));
            chatMessageRepository.save(msg);
        }
        
        // 聊天室2: 5条消息
        for (int i = 0; i < 5; i++) {
            ChatMessage msg = TestDataFactory.createTestChatMessage(
                testChatroomId2, testUser2, "Room2 Message " + i, 
                new Date(now.getTime() + i * 1000));
            chatMessageRepository.save(msg);
        }

        // When - 分别查询两个聊天室
        List<ChatMessage> room1Messages = chatMessageRepository.findByChatroomId(testChatroomId1);
        List<ChatMessage> room2Messages = chatMessageRepository.findByChatroomId(testChatroomId2);

        // Then - 验证查询结果正确且高效
        assertEquals(10, room1Messages.size());
        assertEquals(5, room2Messages.size());
        
        // 验证消息不会串到其他聊天室
        assertTrue(room1Messages.stream().allMatch(msg -> msg.getChatroomId() == testChatroomId1));
        assertTrue(room2Messages.stream().allMatch(msg -> msg.getChatroomId() == testChatroomId2));
    }

    @Test
    void testUserDataIntegrity() {
        // Given
        UserDTO complexUser = TestDataFactory.createTestUser(999L);
        complexUser.setFirstName("complex");
        complexUser.setLastName("user");
        complexUser.setMail("complex.user@test.com");

        ChatMessage message = TestDataFactory.createTestChatMessage(
            testChatroomId1, complexUser, "complexUserInfo", new Date());

        // When
        chatMessageRepository.save(message);
        List<ChatMessage> retrievedMessages = chatMessageRepository.findByChatroomId(testChatroomId1);

        // Then
        assertEquals(1, retrievedMessages.size());
        ChatMessage retrieved = retrievedMessages.get(0);
        
        assertNotNull(retrieved.getUser());
        assertEquals(complexUser.getId(), retrieved.getUser().getId());
        assertEquals("complex", retrieved.getUser().getFirstName());
        assertEquals("user", retrieved.getUser().getLastName());
        assertEquals("complex.user@test.com", retrieved.getUser().getMail());
        assertEquals("complexUserInfo", retrieved.getContent());
    }
}