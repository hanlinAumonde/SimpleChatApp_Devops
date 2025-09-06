package com.devStudy.chatapp.gateway.dto;

public class UserInfo {
    private Long id;
    private String mail;
    private String firstName;
    private String lastName;

    public UserInfo() {}

    public UserInfo(Long id, String mail, String username, String role) {
        this.id = id;
        this.mail = mail;
        this.firstName = username;
        this.lastName = role;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
