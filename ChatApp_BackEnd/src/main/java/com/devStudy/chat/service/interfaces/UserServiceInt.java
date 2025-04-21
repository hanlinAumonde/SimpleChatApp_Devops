package com.devStudy.chat.service.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import com.devStudy.chat.dto.CreateCompteDTO;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.model.User;

import java.util.Map;
import java.util.Optional;

public interface UserServiceInt {

    UserDTO getLoggedUser(String email);

    long getUserId(HttpServletRequest request);

    Page<User> findAllUsersByPage(int page);

    CreateCompteDTO addUser(CreateCompteDTO user);

    Page<User> findAllUsersNotAdminByPage(int page);

    Page<UserDTO> findAllOtherUsersNotAdminByPage(int page, long userId);

    Page<UserDTO> findUsersInvitedToChatroomByPage(long chatroomId, int page);

    Page<UserDTO> findUsersNotInvitedToChatroomByPage(long chatroomId, long userId, int page);

    int incrementFailedAttemptsOfUser(String userEmail);

    void lockUserAndResetFailedAttempts(String userEmail);

    Optional<User> findUserById(long userId);

    Optional<User> findUserOrAdmin(String email, boolean isAdmin);

    //Map<String, String> sendResetPwdEmailRequestToMQ(String email);
    //Map<String, String> sendResetPasswordEmail(String email);

    boolean resetPassword(String jwtToken, String password);

    void resetFailedAttemptsOfUser(String username);
}