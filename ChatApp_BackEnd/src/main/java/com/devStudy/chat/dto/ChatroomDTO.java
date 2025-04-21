package com.devStudy.chat.dto;

/**
 * Cette classe permet de représenter une chatroom avec moins d'informations que la classe Chatroom
 */
public class ChatroomDTO {
    public long id;
    public String titre;
    public String description;
    public boolean isActif;
    //getters and setters
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
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

	public boolean getIsActif() {
		return this.isActif;
	}
	
	public void setIsActif(boolean isActif) {
		this.isActif = isActif;
	}
}
