package com.devStudy.chatapp.auth.dto;

import com.devStudy.chatapp.auth.model.User;

public class DTOMapper {
	
    public static UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setMail(user.getMail());
        return dto;
    }
}