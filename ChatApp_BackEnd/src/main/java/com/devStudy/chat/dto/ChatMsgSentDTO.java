package com.devStudy.chat.dto;

public class ChatMsgSentDTO {
    String message;
    String firstName;
    String lastName;

    public ChatMsgSentDTO() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstname(String username) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
