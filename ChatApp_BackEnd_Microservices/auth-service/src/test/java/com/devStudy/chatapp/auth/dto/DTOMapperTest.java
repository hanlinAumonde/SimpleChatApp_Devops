package com.devStudy.chatapp.auth.dto;

import com.devStudy.chatapp.auth.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DTOMapperTest {

    @Test
    void testToUserDTO_WithValidUser() {
        User user = new User();
        user.setId(123L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setMail("john.doe@test.com");
        user.setPwd("encodedPassword");
        user.setAdmin(false);
        user.setActive(true);
        
        UserDTO result = DTOMapper.toUserDTO(user);
        
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(), result.getLastName());
        assertEquals(user.getMail(), result.getMail());
    }

    @Test
    void testToUserDTO_WithEmptyUser() {
        User user = new User();
        
        UserDTO result = DTOMapper.toUserDTO(user);
        
        assertNotNull(result);
        assertEquals(0L, result.getId());
        assertNull(result.getFirstName());
        assertNull(result.getLastName());
        assertNull(result.getMail());
    }

    @Test
    void testToUserDTO_WithNullFields() {
        User user = new User();
        user.setId(456L);
        user.setFirstName(null);
        user.setLastName(null);
        user.setMail(null);
        
        UserDTO result = DTOMapper.toUserDTO(user);
        
        assertNotNull(result);
        assertEquals(456L, result.getId());
        assertNull(result.getFirstName());
        assertNull(result.getLastName());
        assertNull(result.getMail());
    }

    @Test
    void testToUserDTO_WithEmptyStrings() {
        User user = new User();
        user.setId(789L);
        user.setFirstName("");
        user.setLastName("");
        user.setMail("");
        
        UserDTO result = DTOMapper.toUserDTO(user);
        
        assertNotNull(result);
        assertEquals(789L, result.getId());
        assertEquals("", result.getFirstName());
        assertEquals("", result.getLastName());
        assertEquals("", result.getMail());
    }

    @Test
    void testToUserDTO_DoesNotExposePassword() {
        User user = new User();
        user.setId(100L);
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setMail("admin@test.com");
        user.setPwd("secretPassword");
        user.setAdmin(true);
        
        UserDTO result = DTOMapper.toUserDTO(user);
        
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(), result.getLastName());
        assertEquals(user.getMail(), result.getMail());
    }
}