package com.devStudy.chat.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static com.devStudy.chat.service.utils.ConstantValues.CreationSuccess;
import static com.devStudy.chat.service.utils.ConstantValues.CompteExist;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import com.devStudy.chat.dao.UserRepository;
import com.devStudy.chat.dto.CreateCompteDTO;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.model.User;
import com.devStudy.chat.service.implementations.EmailService;
import com.devStudy.chat.service.implementations.JwtTokenService;
import com.devStudy.chat.service.implementations.UserService;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserServiceTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceTest.class);
	    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private EmailService emailService;
    
    @MockBean
    private JwtTokenService tokenService;
    
    @MockBean
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserService userService;
    
    // 测试数据准备
    private User testUser;
    private CreateCompteDTO testCreateCompteDTO;
    
    @BeforeAll
	void TestStart() {
		LOGGER.info("-----------------------------------------Test UserService started-----------------------------------------------");
	}
	
	@AfterAll
	void TestEnd() {
		LOGGER.info("-----------------------------------------Test UserService ended-------------------------------------------------");
	}
    
    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setMail("test@example.com");
        testUser.setPwd("encodedPassword");
        testUser.setAdmin(false);
        testUser.setActive(true);
        testUser.setFailedAttempts(0);
        
        testCreateCompteDTO = new CreateCompteDTO();
        testCreateCompteDTO.setFirstName("New");
        testCreateCompteDTO.setLastName("User");
        testCreateCompteDTO.setMail("new@example.com");
        testCreateCompteDTO.setPassword("password123");
        
        // 重置安全上下文
        SecurityContextHolder.clearContext();
    }
    
    //---------------------------------------------测试用户查询相关方法-----------------------------------------
    @Test
    void testFindUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        
        Optional<User> result1 = userService.findUserById(1L);
        assertTrue(result1.isPresent());
        assertEquals("Test", result1.get().getFirstName());
        verify(userRepository).findById(1L);
        
        Optional<User> result2 = userService.findUserById(2L);
        assertTrue(result2.isEmpty());
        verify(userRepository).findById(2L);
    }
    
    @Test
    void testFindUserOrAdmin() {
        when(userRepository.findByMailAndAdmin("test@example.com", false))
            .thenReturn(Optional.of(testUser));
        
        Optional<User> result = userService.findUserOrAdmin("test@example.com", false);
        
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getMail());
        verify(userRepository).findByMailAndAdmin("test@example.com", false);
    }
    
//    @Test
//    void testGetLoggedUser() {
//        // 创建mock认证对象和安全上下文对象,以通过when来模拟其行为
//        Authentication authentication = mock(Authentication.class);
//        SecurityContext securityContext = mock(SecurityContext.class);
//
//        when(securityContext.getAuthentication()).thenReturn(authentication);
//        when(authentication.getPrincipal()).thenReturn(testUser);
//        SecurityContextHolder.setContext(securityContext);
//
//        UserDTO result = userService.getLoggedUser();
//
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("Test", result.getFirstName());
//        assertEquals("User", result.getLastName());
//        assertEquals("test@example.com", result.getMail());
//        verify(securityContext).getAuthentication();
//        verify(authentication).getPrincipal();
//    }
    
    //---------------------------------------------测试用户管理相关方法-----------------------------------------
    @Test
    void testAddUserSuccessfully() {
    	//测试成功加入用户
        when(userRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        
        CreateCompteDTO result = userService.addUser(testCreateCompteDTO);
        
        assertEquals(CreationSuccess, result.getCreateMsg());
        verify(userRepository).save(argThat(user -> 
    		"New".equals(user.getFirstName()) &&
    		"User".equals(user.getLastName()) &&
    		"new@example.com".equals(user.getMail()) &&
    		"encodedPassword123".equals(user.getPwd()) &&
    		!user.isAdmin() && user.isActive() &&
    		user.getFailedAttempts() == 0
        ));
	}
    
    @Test
    void testAddUserFailed() {
    	//测试加入已存在用户
    	testUser.setMail("new@example.com");
		when(userRepository.findAll()).thenReturn(List.of(testUser));
		
		CreateCompteDTO resultFailed = userService.addUser(testCreateCompteDTO);
		assertEquals(CompteExist, resultFailed.getCreateMsg());
		verify(userRepository, never()).save(any(User.class));
    }
    
    //---------------------------------------------用户认证相关测试-----------------------------------------
    @Test
    void testLoadUserByUsernameSuccessfully() {
        when(userRepository.findByMailAndAdmin("test@example.com", false))
            .thenReturn(Optional.of(testUser));
        UserDetails result = userService.loadUserByUsername("test@example.com");
        assertNotNull(result);
        assertEquals("test@example.com", result.getUsername());
        verify(userRepository).findByMailAndAdmin("test@example.com", false);
    }
    
    @Test
    void testLoadUserByUserNameFailed() {
		when(userRepository.findByMailAndAdmin("test@example.com", false))
			.thenReturn(Optional.empty());
		UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("test@example.com"),
				"UserNotFoundException should be thrown");
		assertEquals("Identifiants incorrects", thrown.getMessage());
        verify(userRepository).findByMailAndAdmin("test@example.com", false);
    }
    
    @Test
    void testLoadUserByUserNameBlocked() {
		testUser.setActive(false);
		when(userRepository.findByMailAndAdmin("test@example.com", false))
		    .thenReturn(Optional.of(testUser));
		UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("test@example.com"),
				"UserNotFoundException should be thrown");
		assertEquals("Compté bloqué", thrown.getMessage());
        verify(userRepository).findByMailAndAdmin("test@example.com", false);
    }
    
    @Test
    void testIncrementFailedAttemptsOfUser() {
        testUser.setFailedAttempts(2);
        when(userRepository.findByMailAndAdmin("test@example.com", false))
            .thenReturn(Optional.of(testUser));
        
        int result = userService.incrementFailedAttemptsOfUser("test@example.com");
        
        assertEquals(3, result);
        verify(userRepository).updateFailedAttempts("test@example.com", 3);
    }
    
    @Test
    void testResetFailedAttemptsOfUser() {
        userService.resetFailedAttemptsOfUser("test@example.com");
        verify(userRepository).updateFailedAttempts("test@example.com", 0);
    }
    
    @Test
    void testLockUserAndResetFailedAttempts() {
        userService.lockUserAndResetFailedAttempts("test@example.com");
        
        verify(userRepository).updateActive("test@example.com", false);
        verify(userRepository).updateFailedAttempts("test@example.com", 0);
    }
    
    //---------------------------------------------测试用户密码重置相关方法-----------------------------------------
    @Test
    void testResetPassword_Success() {
        when(tokenService.validateTokenAndGetEmail("valid-token"))
            .thenReturn("test@example.com");
        when(passwordEncoder.encode("newPassword"))
            .thenReturn("encodedNewPassword");
        
        boolean result = userService.resetPassword("valid-token", "newPassword");
        
        assertTrue(result);
        verify(userRepository).updatePwd("test@example.com", "encodedNewPassword");
    }

    @Test
    void testResetPassword_InvalidToken() {
        when(tokenService.validateTokenAndGetEmail("invalid-token"))
            .thenReturn("");
        
        boolean result = userService.resetPassword("invalid-token", "newPassword");
        
        assertFalse(result);
        verify(userRepository, never()).updatePwd(anyString(), anyString());
    }
    
    //---------------------------------------------测试分页查找相关方法-----------------------------------------
    @Test
    void testFindAllUsersByPage() {
        Page<User> mockPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(mockPage);
        
        Page<User> result = userService.findAllUsersByPage(0);
        
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(PageRequest.class));
    }

    @Test
    void testFindAllUsersNotAdminByPage() {
        Page<User> mockPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findByAdmin(eq(false), any(PageRequest.class))).thenReturn(mockPage);
        
        Page<User> result = userService.findAllUsersNotAdminByPage(0);
        
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByAdmin(eq(false), any(PageRequest.class));
    }

    @Test
    void testFindAllOtherUsersNotAdminByPage() {
        Page<User> mockPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAllOtherUsersNotAdminByPage(eq(2L), any(PageRequest.class)))
            .thenReturn(mockPage);
        
        Page<UserDTO> result = userService.findAllOtherUsersNotAdminByPage(0, 2L);
        
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAllOtherUsersNotAdminByPage(eq(2L), any(PageRequest.class));
    }

    @Test
    void testFindUsersInvitedToChatroomByPage() {
        Page<User> mockPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findUsersInvitedToChatroomByPage(eq(1L), any(PageRequest.class)))
            .thenReturn(mockPage);
        
        Page<UserDTO> result = userService.findUsersInvitedToChatroomByPage(1L, 0);
        
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findUsersInvitedToChatroomByPage(eq(1L), any(PageRequest.class));
    }
}
