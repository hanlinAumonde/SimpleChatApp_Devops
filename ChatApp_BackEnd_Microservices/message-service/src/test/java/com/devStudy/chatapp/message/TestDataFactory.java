package com.devStudy.chatapp.message;

import com.devStudy.chatapp.message.dto.UserDTO;
import com.devStudy.chatapp.message.model.ChatMessage;
import org.bson.types.ObjectId;

import java.util.Date;

/**
 * 测试数据工厂类，用于创建测试所需的对象
 */
public class TestDataFactory {

    public static UserDTO createTestUser(long userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        user.setFirstName("Test");
        user.setLastName("User" + userId);
        user.setMail("testuser" + userId + "@test.com");
        return user;
    }

    public static ChatMessage createTestChatMessage(long chatroomId, UserDTO user, String content, Date timestamp) {
        ChatMessage message = new ChatMessage();
        message.setId(new ObjectId());
        message.setChatroomId(chatroomId);
        message.setUser(user);
        message.setContent(content);
        message.setTimestamp(timestamp);
        return message;
    }

    public static Date createTestDate(int year, int month, int day, int hour, int minute) {
        return new Date(year - 1900, month - 1, day, hour, minute);
    }
}