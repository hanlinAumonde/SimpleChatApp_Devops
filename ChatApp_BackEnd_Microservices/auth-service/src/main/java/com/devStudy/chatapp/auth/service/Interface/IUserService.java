package com.devStudy.chatapp.auth.service.Interface;

import com.devStudy.chatapp.auth.dto.CreateCompteDTO;
import com.devStudy.chatapp.auth.dto.UserDTO;
import com.devStudy.chatapp.auth.model.User;

import java.util.Optional;

public interface IUserService {
    UserDTO getLoggedUser(String email);
    CreateCompteDTO addUser(CreateCompteDTO user);
    int incrementFailedAttemptsOfUser(String userEmail);
    void lockUserAndResetFailedAttempts(String userEmail);
    Optional<User> findUserOrAdmin(String email, boolean isAdmin);
    boolean resetPassword(String jwtToken, String password);
    void resetFailedAttemptsOfUser(String username);
}
