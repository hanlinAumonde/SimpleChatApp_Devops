package com.devStudy.chatapp.gateway.dto;

public class UserInfo {
    private Long id;
    private String email;
    private String username;
    private String role;

    public UserInfo() {}

    public UserInfo(Long id, String email, String username, String role) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
