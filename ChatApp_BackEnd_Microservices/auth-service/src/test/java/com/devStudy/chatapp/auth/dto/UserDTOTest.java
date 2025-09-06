package com.devStudy.chatapp.auth.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class UserDTOTest {

    @Test
    void testDefaultConstructor() {
        UserDTO dto = new UserDTO();
        
        assertEquals(0L, dto.getId());
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getMail());
    }

    @Test
    void testSettersAndGetters() {
        UserDTO dto = new UserDTO();
        
        long expectedId = 123L;
        String expectedFirstName = "John";
        String expectedLastName = "Doe";
        String expectedMail = "john.doe@test.com";
        
        dto.setId(expectedId);
        dto.setFirstName(expectedFirstName);
        dto.setLastName(expectedLastName);
        dto.setMail(expectedMail);
        
        assertEquals(expectedId, dto.getId());
        assertEquals(expectedFirstName, dto.getFirstName());
        assertEquals(expectedLastName, dto.getLastName());
        assertEquals(expectedMail, dto.getMail());
    }

    @Test
    void testNullValues() {
        UserDTO dto = new UserDTO();
        
        dto.setId(0L);
        dto.setFirstName(null);
        dto.setLastName(null);
        dto.setMail(null);
        
        assertEquals(0L, dto.getId());
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getMail());
    }

    @Test
    void testEmptyStrings() {
        UserDTO dto = new UserDTO();
        
        dto.setFirstName("");
        dto.setLastName("");
        dto.setMail("");
        
        assertEquals("", dto.getFirstName());
        assertEquals("", dto.getLastName());
        assertEquals("", dto.getMail());
    }

    @Test
    void testSerialization() throws Exception {
        UserDTO originalDto = new UserDTO();
        originalDto.setId(456L);
        originalDto.setFirstName("Jane");
        originalDto.setLastName("Smith");
        originalDto.setMail("jane.smith@test.com");

        // 序列化
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalDto);
        oos.close();

        // 反序列化
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        UserDTO deserializedDto = (UserDTO) ois.readObject();
        ois.close();

        // 验证
        assertEquals(originalDto.getId(), deserializedDto.getId());
        assertEquals(originalDto.getFirstName(), deserializedDto.getFirstName());
        assertEquals(originalDto.getLastName(), deserializedDto.getLastName());
        assertEquals(originalDto.getMail(), deserializedDto.getMail());
    }
}