package com.devStudy.chatapp.crud.controller;

import com.devStudy.chatapp.crud.dto.ChatroomDTO;
import com.devStudy.chatapp.crud.dto.ChatroomWithOwnerAndStatusDTO;
import com.devStudy.chatapp.crud.service.Implementation.ChatroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devStudy.chatapp.crud.dto.UserDTO;
import com.devStudy.chatapp.crud.service.Implementation.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
	
	private final UserService userService;

    private final ChatroomService chatroomService;

    @Autowired
    public UserController(UserService userService, ChatroomService chatroomService) {
        this.userService = userService;
        this.chatroomService = chatroomService;
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

    // User-related endpoints integrated into this controller
    @GetMapping("/{userId}/chatrooms/owned")
    public ResponseEntity<Page<ChatroomDTO>> getChatroomsOwnedByUser(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0")int page,
            @RequestHeader("X-User-Id") String userIdHeader){
        long currentUserId = userService.getUserIdFromHeaders(userIdHeader);
        if(userId == currentUserId){
            return ResponseEntity.ok(chatroomService.getChatroomsOwnedOfUserByPage(userId,page));
        }
        return ResponseEntity.status(403).body(Page.empty());
    }

    @GetMapping("/{userId}/chatrooms/joined")
    public ResponseEntity<Page<ChatroomWithOwnerAndStatusDTO>> getChatroomsJoinedByUser(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0")int page,
            @RequestHeader("X-User-Id") String userIdHeader){
        long currentUserId = userService.getUserIdFromHeaders(userIdHeader);
        if(userId == currentUserId){
            return ResponseEntity.ok(chatroomService.getChatroomsJoinedOfUserByPage(userId, false, page));
        }
        return ResponseEntity.status(403).body(Page.empty());
    }
}