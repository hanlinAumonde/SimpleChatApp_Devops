package com.devStudy.chatapp.auth.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateCompteDTOTest {

    @Test
    void testDefaultConstructor() {
        CreateCompteDTO dto = new CreateCompteDTO();
        
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getMail());
        assertNull(dto.getPassword());
        assertNull(dto.getCreateMsg());
    }

    @Test
    void testSettersAndGetters() {
        CreateCompteDTO dto = new CreateCompteDTO();
        
        String expectedFirstName = "John";
        String expectedLastName = "Doe";
        String expectedMail = "john.doe@test.com";
        String expectedPassword = "password123";
        String expectedCreateMsg = "Account created successfully";
        
        dto.setFirstName(expectedFirstName);
        dto.setLastName(expectedLastName);
        dto.setMail(expectedMail);
        dto.setPassword(expectedPassword);
        dto.setCreateMsg(expectedCreateMsg);
        
        assertEquals(expectedFirstName, dto.getFirstName());
        assertEquals(expectedLastName, dto.getLastName());
        assertEquals(expectedMail, dto.getMail());
        assertEquals(expectedPassword, dto.getPassword());
        assertEquals(expectedCreateMsg, dto.getCreateMsg());
    }

    @Test
    void testNullValues() {
        CreateCompteDTO dto = new CreateCompteDTO();
        
        dto.setFirstName(null);
        dto.setLastName(null);
        dto.setMail(null);
        dto.setPassword(null);
        dto.setCreateMsg(null);
        
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getMail());
        assertNull(dto.getPassword());
        assertNull(dto.getCreateMsg());
    }

    @Test
    void testEmptyStrings() {
        CreateCompteDTO dto = new CreateCompteDTO();
        
        dto.setFirstName("");
        dto.setLastName("");
        dto.setMail("");
        dto.setPassword("");
        dto.setCreateMsg("");
        
        assertEquals("", dto.getFirstName());
        assertEquals("", dto.getLastName());
        assertEquals("", dto.getMail());
        assertEquals("", dto.getPassword());
        assertEquals("", dto.getCreateMsg());
    }
}