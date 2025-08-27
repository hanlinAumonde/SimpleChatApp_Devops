package com.devStudy.chatapp.crud.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "firstname", nullable = false)
    private String firstName;

    @Column(name = "lastname", nullable = false)
    private String lastName;

    @Column(name = "mail", unique = true, nullable = false)
    private String mail;

    @Column(name = "is_admin")
    private boolean admin = false;

    @Column(name = "is_active")
    private boolean active = true;

    // 用户创建的聊天室
    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    private Set<Chatroom> createdRooms = new HashSet<>();

    // 用户加入的聊天室
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_chatroom_relationship",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "chatroom_id")
    )
    private Set<Chatroom> joinedRooms = new HashSet<>();

    public User() {}

    public User(long id, String firstName, String lastName, String mail) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mail = mail;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Chatroom> getCreatedRooms() {
        return createdRooms;
    }

    public void setCreatedRooms(Set<Chatroom> createdRooms) {
        this.createdRooms = createdRooms;
    }

    public Set<Chatroom> getJoinedRooms() {
        return joinedRooms;
    }

    public void setJoinedRooms(Set<Chatroom> joinedRooms) {
        this.joinedRooms = joinedRooms;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}