package com.devStudy.chatapp.crud.service;

import com.devStudy.chatapp.crud.dto.UserDTO;
import com.devStudy.chatapp.crud.model.User;
import com.devStudy.chatapp.crud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private Page<User> userPage;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(userService, "DefaultPageSize_Users", 10);

        // 创建测试用户
        user1 = new User();
        user1.setId(1L);
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setMail("john@test.com");
        user1.setAdmin(false);
        user1.setActive(true);

        user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setMail("jane@test.com");
        user2.setAdmin(false);
        user2.setActive(true);

        List<User> userList = Arrays.asList(user1, user2);
        userPage = new PageImpl<>(userList);
    }

    @Test
    void testGetUserIdFromHeaders_ValidId() {
        // 测试有效的用户ID解析
        String userIdHeader = "123";
        long result = userService.getUserIdFromHeaders(userIdHeader);
        
        assertThat(result).isEqualTo(123L);
    }

    @Test
    void testGetUserIdFromHeaders_InvalidId() {
        // 测试无效的用户ID解析
        String invalidHeader = "abc";
        
        assertThatThrownBy(() -> userService.getUserIdFromHeaders(invalidHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid user ID");
    }

    @Test
    void testGetUserIdFromHeaders_EmptyId() {
        // 测试空的用户ID
        String emptyHeader = "";
        
        assertThatThrownBy(() -> userService.getUserIdFromHeaders(emptyHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid user ID");
    }

    @Test
    void testFindAllOtherUsersNotAdminByPage() {
        // 测试查找其他非管理员用户
        long userId = 1L;
        int page = 0;

        when(userRepository.findAllOtherUsersNotAdminByPage(eq(userId), any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserDTO> result = userService.findAllOtherUsersNotAdminByPage(page, userId);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(user1.getId());
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
        assertThat(result.getContent().get(1).getId()).isEqualTo(user2.getId());
        assertThat(result.getContent().get(1).getFirstName()).isEqualTo("Jane");

        verify(userRepository).findAllOtherUsersNotAdminByPage(eq(userId), any(Pageable.class));
    }

    @Test
    void testFindUsersInvitedToChatroomByPage() {
        // 测试查找聊天室邀请的用户
        long chatroomId = 100L;
        int page = 0;

        when(userRepository.findUsersInvitedToChatroomByPage(eq(chatroomId), any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserDTO> result = userService.findUsersInvitedToChatroomByPage(chatroomId, page);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(UserDTO::getId)
                .containsExactly(user1.getId(), user2.getId());

        verify(userRepository).findUsersInvitedToChatroomByPage(eq(chatroomId), any(Pageable.class));
    }

    @Test
    void testFindUsersNotInvitedToChatroomByPage() {
        // 测试查找未邀请到聊天室的用户
        long chatroomId = 100L;
        long userId = 1L;
        int page = 0;

        when(userRepository.findUsersNotInvitedToChatroomByPage(eq(chatroomId), eq(userId), any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserDTO> result = userService.findUsersNotInvitedToChatroomByPage(chatroomId, userId, page);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(UserDTO::getId)
                .containsExactly(user1.getId(), user2.getId());

        verify(userRepository).findUsersNotInvitedToChatroomByPage(eq(chatroomId), eq(userId), any(Pageable.class));
    }

    @Test
    void testPageableSettingGeneration() {
        // 测试分页设置生成
        int page = 2;
        Page<User> emptyPage = new PageImpl<>(List.of());
        
        when(userRepository.findAllOtherUsersNotAdminByPage(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        userService.findAllOtherUsersNotAdminByPage(page, 1L);

        // 验证正确的分页参数被传递
        Pageable expectedPageable = PageRequest.of(page, 10, 
                Sort.sort(User.class).by(User::getFirstName).ascending());
        verify(userRepository).findAllOtherUsersNotAdminByPage(eq(1L), eq(expectedPageable));
    }

    @Test
    void testFindAllOtherUsersNotAdminByPage_EmptyResult() {
        // 测试空结果
        long userId = 1L;
        int page = 0;
        Page<User> emptyPage = new PageImpl<>(List.of());

        when(userRepository.findAllOtherUsersNotAdminByPage(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<UserDTO> result = userService.findAllOtherUsersNotAdminByPage(page, userId);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(userRepository).findAllOtherUsersNotAdminByPage(eq(userId), any(Pageable.class));
    }
}