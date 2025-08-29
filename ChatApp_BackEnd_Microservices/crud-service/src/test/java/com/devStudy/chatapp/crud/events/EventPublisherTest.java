package com.devStudy.chatapp.crud.events;

import com.devStudy.chatapp.crud.config.RabbitMQConfig;
import com.devStudy.chatapp.crud.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    private UserDTO addedUser;
    private UserDTO removedUser;
    private List<UserDTO> addedMembers;
    private List<UserDTO> removedMembers;

    @BeforeEach
    void setUp() {
        addedUser = new UserDTO();
        addedUser.setId(1L);
        addedUser.setFirstName("John");
        addedUser.setLastName("Doe");
        addedUser.setMail("john@test.com");

        removedUser = new UserDTO();
        removedUser.setId(2L);
        removedUser.setFirstName("Jane");
        removedUser.setLastName("Smith");
        removedUser.setMail("jane@test.com");

        addedMembers = Collections.singletonList(addedUser);
        removedMembers = Collections.singletonList(removedUser);
    }

    @Test
    void testHandleChatroomMemberChangeEvent_Success() {
        // 测试成功处理聊天室成员变更事件
        long chatroomId = 100L;
        ChangeChatroomMemberEvent event = new ChangeChatroomMemberEvent(chatroomId, addedMembers, removedMembers);

        eventPublisher.handleChatroomMemberChangeEvent(event);

        // 验证RabbitTemplate被调用
        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY),
                messageCaptor.capture()
        );

        // 验证消息内容
        Map capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.get("chatroomId")).isEqualTo(chatroomId);
        assertThat(capturedMessage.get("addedMembers")).isEqualTo(addedMembers);
        assertThat(capturedMessage.get("removedMembers")).isEqualTo(removedMembers);
        assertThat(capturedMessage.get("eventType")).isEqualTo("CHATROOM_MEMBER_CHANGE");
    }

    @Test
    void testHandleChatroomMemberChangeEvent_EmptyMembers() {
        // 测试空成员列表的事件
        long chatroomId = 100L;
        List<UserDTO> emptyList = Collections.emptyList();
        ChangeChatroomMemberEvent event = new ChangeChatroomMemberEvent(chatroomId, emptyList, emptyList);

        eventPublisher.handleChatroomMemberChangeEvent(event);

        // 验证RabbitTemplate被调用
        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY),
                messageCaptor.capture()
        );

        // 验证消息内容
        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.get("chatroomId")).isEqualTo(chatroomId);
        assertThat(capturedMessage.get("addedMembers")).isEqualTo(emptyList);
        assertThat(capturedMessage.get("removedMembers")).isEqualTo(emptyList);
        assertThat(capturedMessage.get("eventType")).isEqualTo("CHATROOM_MEMBER_CHANGE");
    }

    @Test
    void testHandleChatroomMemberChangeEvent_OnlyAddedMembers() {
        // 测试只有添加成员的事件
        long chatroomId = 100L;
        List<UserDTO> emptyList = Collections.emptyList();
        ChangeChatroomMemberEvent event = new ChangeChatroomMemberEvent(chatroomId, addedMembers, emptyList);

        eventPublisher.handleChatroomMemberChangeEvent(event);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY),
                messageCaptor.capture()
        );

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.get("addedMembers")).isEqualTo(addedMembers);
        assertThat(capturedMessage.get("removedMembers")).isEqualTo(emptyList);
    }

    @Test
    void testHandleChatroomMemberChangeEvent_OnlyRemovedMembers() {
        // 测试只有移除成员的事件
        long chatroomId = 100L;
        List<UserDTO> emptyList = Collections.emptyList();
        ChangeChatroomMemberEvent event = new ChangeChatroomMemberEvent(chatroomId, emptyList, removedMembers);

        eventPublisher.handleChatroomMemberChangeEvent(event);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY),
                messageCaptor.capture()
        );

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.get("addedMembers")).isEqualTo(emptyList);
        assertThat(capturedMessage.get("removedMembers")).isEqualTo(removedMembers);
    }

    @Test
    void testHandleChatroomMemberChangeEvent_RabbitTemplateException() {
        // 测试RabbitTemplate抛出异常的情况
        long chatroomId = 100L;
        ChangeChatroomMemberEvent event = new ChangeChatroomMemberEvent(chatroomId, addedMembers, removedMembers);

        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Map.class));

        // 事件处理器不应该抛出异常，而是记录日志
        eventPublisher.handleChatroomMemberChangeEvent(event);

        // 验证RabbitTemplate被调用
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY),
                any(Map.class)
        );
    }

    @Test
    void testHandleRemoveChatroomEvent_Success() {
        // 测试成功处理聊天室删除事件
        long chatroomId = 100L;
        RemoveChatroomEvent event = new RemoveChatroomEvent(chatroomId);

        eventPublisher.handleRemoveChatroomEvent(event);

        // 验证RabbitTemplate被调用
        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_REMOVE_ROUTING_KEY),
                messageCaptor.capture()
        );

        // 验证消息内容
        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.get("chatroomId")).isEqualTo(chatroomId);
        assertThat(capturedMessage.get("eventType")).isEqualTo("CHATROOM_REMOVE");
    }

    @Test
    void testHandleRemoveChatroomEvent_RabbitTemplateException() {
        // 测试RabbitTemplate抛出异常的情况
        long chatroomId = 100L;
        RemoveChatroomEvent event = new RemoveChatroomEvent(chatroomId);

        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Map.class));

        // 事件处理器不应该抛出异常，而是记录日志
        eventPublisher.handleRemoveChatroomEvent(event);

        // 验证RabbitTemplate被调用
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_REMOVE_ROUTING_KEY),
                any(Map.class)
        );
    }

    @Test
    void testMultipleUsersInChangeEvent() {
        // 测试多个用户的成员变更事件
        UserDTO user3 = new UserDTO();
        user3.setId(3L);
        user3.setFirstName("Bob");
        user3.setLastName("Wilson");
        user3.setMail("bob@test.com");

        UserDTO user4 = new UserDTO();
        user4.setId(4L);
        user4.setFirstName("Alice");
        user4.setLastName("Brown");
        user4.setMail("alice@test.com");

        List<UserDTO> multipleAddedMembers = Arrays.asList(addedUser, user3);
        List<UserDTO> multipleRemovedMembers = Arrays.asList(removedUser, user4);

        long chatroomId = 100L;
        ChangeChatroomMemberEvent event = new ChangeChatroomMemberEvent(chatroomId, multipleAddedMembers, multipleRemovedMembers);

        eventPublisher.handleChatroomMemberChangeEvent(event);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY),
                messageCaptor.capture()
        );

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.get("chatroomId")).isEqualTo(chatroomId);
        assertThat((List<?>) capturedMessage.get("addedMembers")).hasSize(2);
        assertThat((List<?>) capturedMessage.get("removedMembers")).hasSize(2);
        assertThat(capturedMessage.get("eventType")).isEqualTo("CHATROOM_MEMBER_CHANGE");
    }

    @Test
    void testEventPublisherAnnotations() {
        // 验证EventPublisher类有正确的注解
        assertThat(EventPublisher.class.isAnnotationPresent(org.springframework.stereotype.Component.class))
                .isTrue();
    }

    @Test
    void testEventListenerAnnotations() throws NoSuchMethodException {
        // 验证事件监听器方法有正确的注解
        assertThat(EventPublisher.class
                .getMethod("handleChatroomMemberChangeEvent", ChangeChatroomMemberEvent.class)
                .isAnnotationPresent(org.springframework.transaction.event.TransactionalEventListener.class))
                .isTrue();

        assertThat(EventPublisher.class
                .getMethod("handleRemoveChatroomEvent", RemoveChatroomEvent.class)
                .isAnnotationPresent(org.springframework.transaction.event.TransactionalEventListener.class))
                .isTrue();
    }

    @Test
    void testMessageStructure() {
        // 测试消息结构的完整性
        long chatroomId = 100L;
        ChangeChatroomMemberEvent event = new ChangeChatroomMemberEvent(chatroomId, addedMembers, removedMembers);

        eventPublisher.handleChatroomMemberChangeEvent(event);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY),
                messageCaptor.capture()
        );

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        
        // 验证消息包含所有必需的字段
        assertThat(capturedMessage).containsKeys("chatroomId", "addedMembers", "removedMembers", "eventType");
        
        // 验证数据类型
        assertThat(capturedMessage.get("chatroomId")).isInstanceOf(Long.class);
        assertThat(capturedMessage.get("addedMembers")).isInstanceOf(List.class);
        assertThat(capturedMessage.get("removedMembers")).isInstanceOf(List.class);
        assertThat(capturedMessage.get("eventType")).isInstanceOf(String.class);
    }
}