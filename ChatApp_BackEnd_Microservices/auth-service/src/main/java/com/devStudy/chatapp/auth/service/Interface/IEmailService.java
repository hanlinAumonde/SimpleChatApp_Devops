package com.devStudy.chatapp.auth.service.Interface;

public interface IEmailService {
    void sendSimpleMessage(String to, String subject, String text);
}
