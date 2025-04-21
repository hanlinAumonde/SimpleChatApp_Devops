package com.devStudy.chat.dto;

import java.util.List;

public class ModifyChatroomRequestDTO {
	public String titre;
	public String description;
	public String startDate;
	public int duration;
	public List<UserDTO> usersInvited;
	public List<UserDTO> usersRemoved;
	
	public String getTitre() {
		return this.titre;
	}
	
	public void setTitre(String titre) {
		this.titre = titre;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getStartDate() {
		return this.startDate;
	}
	
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	
	public int getDuration() {
		return this.duration;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public List<UserDTO> getListAddedUsers() {
		return this.usersInvited;
	}
	
	public void setListAddedUsers(List<UserDTO> listAddedUsers) {
		this.usersInvited = listAddedUsers;
	}
	
	public List<UserDTO> getListRemovedUsers() {
		return this.usersRemoved;
	}
	
	public void setListRemovedUsers(List<UserDTO> listRemovedUsers) {
		this.usersRemoved = listRemovedUsers;
	}
}
