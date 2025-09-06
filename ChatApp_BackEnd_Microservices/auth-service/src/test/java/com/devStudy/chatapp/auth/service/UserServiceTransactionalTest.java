package com.devStudy.chatapp.auth.service;

import com.devStudy.chatapp.auth.dto.CreateCompteDTO;
import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.repository.UserRepository;
import com.devStudy.chatapp.auth.service.Implementation.JwtTokenService;
import com.devStudy.chatapp.auth.service.Implementation.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.NoSuchElementException;

import static com.devStudy.chatapp.auth.utils.ConstantValues.CompteExist;
import static com.devStudy.chatapp.auth.utils.ConstantValues.CreationSuccess;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserServiceTransactionalTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserService userService;
    private User existingUser;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public JwtTokenService jwtTokenService() {
            return new JwtTokenService();
        }
    }

    @BeforeEach
    void setUp() {
        userService = new UserService(passwordEncoder, userRepository, new JwtTokenService());
        
        userRepository.deleteAll();
        
        existingUser = new User();
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");
        existingUser.setMail("john.doe@test.com");
        existingUser.setPwd(passwordEncoder.encode("password123"));
        existingUser.setAdmin(false);
        existingUser.setActive(true);
        existingUser.setFailedAttempts(0);
        
        userRepository.save(existingUser);
        userRepository.flush(); // 确保保存到数据库
    }

    @Test
    void testAddUser_NewUser_Success() {
        CreateCompteDTO newUserDTO = new CreateCompteDTO();
        newUserDTO.setFirstName("Jane");
        newUserDTO.setLastName("Smith");
        newUserDTO.setMail("jane.smith@test.com");
        newUserDTO.setPassword("password123");

        CreateCompteDTO result = userService.addUser(newUserDTO);

        assertEquals(CreationSuccess, result.getCreateMsg());
        
        // 验证用户确实保存到数据库
        User savedUser = userRepository.findByMailAndAdmin("jane.smith@test.com", false)
                .orElseThrow(() -> new AssertionError("User should be saved"));
        
        assertEquals("Jane", savedUser.getFirstName());
        assertEquals("Smith", savedUser.getLastName());
        assertEquals("jane.smith@test.com", savedUser.getMail());
        assertFalse(savedUser.isAdmin());
        assertTrue(savedUser.isActive());
        assertEquals(0, savedUser.getFailedAttempts());
        
        // 验证密码已加密
        assertTrue(passwordEncoder.matches("password123", savedUser.getPwd()));
    }

    @Test
    void testAddUser_DuplicateEmail_Failure() {
        CreateCompteDTO duplicateUserDTO = new CreateCompteDTO();
        duplicateUserDTO.setFirstName("Another");
        duplicateUserDTO.setLastName("User");
        duplicateUserDTO.setMail("john.doe@test.com"); // 已存在的邮箱
        duplicateUserDTO.setPassword("password456");

        CreateCompteDTO result = userService.addUser(duplicateUserDTO);

        assertEquals(CompteExist, result.getCreateMsg());
        
        // 验证只有一个用户存在
        long userCount = userRepository.count();
        assertEquals(1, userCount);
        
        // 验证原用户信息未被修改
        User originalUser = userRepository.findByMailAndAdmin("john.doe@test.com", false)
                .orElseThrow();
        assertEquals("John", originalUser.getFirstName());
        assertEquals("Doe", originalUser.getLastName());
    }

    @Test
    void testIncrementFailedAttemptsOfUser_Success() {
        int initialAttempts = existingUser.getFailedAttempts();

        int result = userService.incrementFailedAttemptsOfUser("john.doe@test.com");

        assertEquals(initialAttempts + 1, result);
        
        // 验证数据库中的值已更新
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, userRepository.findByMailAndAdmin(existingUser.getMail(), false)
                .orElseThrow().getId());
        assertEquals(initialAttempts + 1, updatedUser.getFailedAttempts());
    }

    @Test
    void testIncrementFailedAttemptsOfUser_UserNotFound() {
        assertThrows(NoSuchElementException.class, () ->
                userService.incrementFailedAttemptsOfUser("nonexistent@test.com"));
    }

    @Test
    void testLockUserAndResetFailedAttempts() {
        // 首先增加失败次数
        userService.incrementFailedAttemptsOfUser("john.doe@test.com");

        entityManager.clear();
        // 验证失败次数已增加
        User userWithFailures = userRepository.findByMailAndAdmin("john.doe@test.com", false)
                .orElseThrow();
        assertTrue(userWithFailures.getFailedAttempts() > 0);
        
        // 锁定用户并重置失败次数
        userService.lockUserAndResetFailedAttempts("john.doe@test.com");

        entityManager.clear();
        // 验证用户被锁定且失败次数重置
        User lockedUser = userRepository.findByMailAndAdmin("john.doe@test.com", false)
                .orElseThrow();
        assertFalse(lockedUser.isActive());
        assertEquals(0, lockedUser.getFailedAttempts());
    }

    @Test
    void testResetFailedAttemptsOfUser() {
        // 首先增加失败次数
        userService.incrementFailedAttemptsOfUser("john.doe@test.com");

        entityManager.clear();
        // 验证失败次数已增加
        User userWithFailures = userRepository.findByMailAndAdmin("john.doe@test.com", false)
                .orElseThrow();
        assertTrue(userWithFailures.getFailedAttempts() > 0);
        
        // 重置失败次数
        userService.resetFailedAttemptsOfUser("john.doe@test.com");

        entityManager.clear();
        // 验证失败次数已重置
        User resetUser = userRepository.findByMailAndAdmin("john.doe@test.com", false)
                .orElseThrow();
        assertEquals(0, resetUser.getFailedAttempts());
    }
}