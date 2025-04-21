package com.devStudy.chat.dto;

public class ChatroomWithOwnerAndStatusDTO extends ChatroomDTO {
	public UserDTO owner;
	public boolean isActif;
	//getters and setters
	public UserDTO getOwner() {
		return this.owner;
	}

	public void setOwner(UserDTO owner) {
		this.owner = owner;
	}
	
	public boolean getIsActif() {
		return this.isActif;
	}
	
	public void setIsActif(boolean isActif) {
		this.isActif = isActif;
	}
}
