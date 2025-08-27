package com.devStudy.chatapp.crud.dto;

import java.util.List;

public class ChatroomRequestDTO {
    public String titre;
    public String description;
    public String startDate;
    public int duration;
    public List<UserDTO> usersInvited;

    public String getTitre() {
        return this.titre;
    }
    public void setTitre(String titre) {
        this.titre= titre;
    }
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description= description;
    }
    public int getDuration() {
        return this.duration;
    }
    public void setDuration_days(int duration) {
        this.duration= duration;
    }
    public List<UserDTO> getUsersInvited() {
        return this.usersInvited;
    }
    public void setUsersInvited(List<UserDTO> usersInvited) {
        this.usersInvited= usersInvited;
    }
    public String getStartDate() {
        return this.startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate= startDate;
    }
}