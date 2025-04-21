package com.devStudy.chat.dto;

import java.util.List;

/**
 * Cette classe permet de récupérer les informations d'une chatroom depuis le client
 */
public class ChatroomRequestDTO {
    public String titre;
    public String description;
    public String startDate;
    public int duration;
    public List<UserDTO> usersInvited;

    //getters and setters
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
