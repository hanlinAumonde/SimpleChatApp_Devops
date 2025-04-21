package com.devStudy.chat.service.interfaces;

public interface EmailServiceInt {

    // envoyer un message simple à l'adresse email spécifiée
    void sendSimpleMessage(String to, String subject, String text);

}
