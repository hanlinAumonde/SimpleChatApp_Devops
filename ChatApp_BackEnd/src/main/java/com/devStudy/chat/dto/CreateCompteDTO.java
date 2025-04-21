package com.devStudy.chat.dto;

public class CreateCompteDTO {
	
	private String firstName;
	
	private String lastName;
	
	private String mail;
	
	private String password;
	
	private String createMsg;
	
	public CreateCompteDTO() {}
	
	// Getters and Setters
	
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
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String getCreateMsg() {
		return createMsg;
	}
	
	public void setCreateMsg(String createMsg) {
		this.createMsg = createMsg;
	}
}
