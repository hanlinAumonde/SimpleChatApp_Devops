package com.devStudy.chatapp.crud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.devStudy.chatapp.crud.dto.UserDTO;
import com.devStudy.chatapp.crud.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
	
	private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/others")
    public ResponseEntity<Page<UserDTO>> getOtherUsers(
            @RequestParam(defaultValue = "0") int page, 
            @RequestHeader("X-User-Id") String userIdHeader) {
        long userId = userService.getUserIdFromHeaders(userIdHeader);
        return ResponseEntity.ok(userService.findAllOtherUsersNotAdminByPage(page, userId));
    }
    
    @GetMapping("/invited-to-chatroom")
    public ResponseEntity<Page<UserDTO>> getUsersInvitedToChatroom(
            @RequestParam long chatroomId, 
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(userService.findUsersInvitedToChatroomByPage(chatroomId, page));
    }
    
    @GetMapping("/not-invited-to-chatroom")
    public ResponseEntity<Page<UserDTO>> getUsersNotInvitedToChatroom(
            @RequestParam long chatroomId, 
            @RequestParam(defaultValue = "0") int page,
            @RequestHeader("X-User-Id") String userIdHeader) {
        long userId = userService.getUserIdFromHeaders(userIdHeader);
        return ResponseEntity.ok(userService.findUsersNotInvitedToChatroomByPage(chatroomId, userId, page));
    }
}