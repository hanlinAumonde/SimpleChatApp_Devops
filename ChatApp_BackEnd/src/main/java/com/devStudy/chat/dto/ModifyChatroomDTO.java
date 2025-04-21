package com.devStudy.chat.dto;

public class ModifyChatroomDTO {
	public long id;
	public String titre;
	public String description;
	public String startDate;
	public int duration;
	public boolean isActif;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getTitre() {
        return titre;
    }
	
	public void setTitre(String titre) {
		this.titre = titre;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getStartDate() {
		return startDate;
	}
	
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public boolean getIsActif() {
		return isActif;
	}
	
	public void setIsActif(boolean isActif) {
		this.isActif = isActif;
	}
}
