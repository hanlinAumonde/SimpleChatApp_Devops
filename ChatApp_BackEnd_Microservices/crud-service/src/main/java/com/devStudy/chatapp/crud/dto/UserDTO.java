package com.devStudy.chatapp.crud.dto;

import java.io.Serializable;

public class UserDTO implements Serializable {
    public long id;
    public String lastName;
    public String firstName;
    public String mail;
    
	public UserDTO() {
	}

    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getLastName() {
        return this.lastName;
    }
    public void setLastName(String lastName) {
        this.lastName= lastName;
    }
    public String getFirstName() {
        return this.firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName= firstName;
    }
    public String getMail() {
        return this.mail;
    }
    public void setMail(String mail) {
        this.mail= mail;
    }
}