package com.devStudy.chat.controller;

import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devStudy.chat.dto.ChatroomDTO;
import com.devStudy.chat.dto.ChatroomWithOwnerAndStatusDTO;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.service.implementations.ChatroomService;
import com.devStudy.chat.service.implementations.UserService;

@RestController
@RequestMapping("/api/users")
public class UserManageController {
	
	@Resource
	private UserService userService;
	
	@Resource
	private ChatroomService chatroomService;

	/**
     * Cette méthode permet d'obtenir tous les informations des autres utilisateurs sauf l'utilisateur connecté
     * Elle va etre utilisée dans le processus du planificateur de chatroom (inviter des utilisateurs)
     */
    @GetMapping("/others")
    public ResponseEntity<Page<UserDTO>> getOtherUsers(@RequestParam(defaultValue ="0")int page, HttpServletRequest request){
        return ResponseEntity.ok(userService.findAllOtherUsersNotAdminByPage(page, userService.getUserId(request)));
    }
    
    /**
     * Cette méthode permet d'obtenir tous les chatrooms créés par l'utilisateur connecté
     */
    @GetMapping("/{userId}/chatrooms/owned")
    public ResponseEntity<Page<ChatroomDTO>> getChatroomsOwnedByUser(@PathVariable long userId, @RequestParam(defaultValue = "0")int page, HttpServletRequest request){
        if(userId == userService.getUserId(request)){
        	return ResponseEntity.ok(chatroomService.getChatroomsOwnedOfUserByPage(userId,page));
        }
        return ResponseEntity.status(403).body(Page.empty());
    }
    
    /**
     * Cette méthode permet d'obtenir tous les chatrooms auxquels l'utilisateur connecté a participé
     */
    @GetMapping("/{userId}/chatrooms/joined")
    public ResponseEntity<Page<ChatroomWithOwnerAndStatusDTO>> getChatroomsJoinedByUser(@PathVariable long userId, @RequestParam(defaultValue = "0")int page, HttpServletRequest request){
        if(userId == userService.getUserId(request)){
        	return ResponseEntity.ok(chatroomService.getChatroomsJoinedOfUserByPage(userId, false, page));
        }
        return ResponseEntity.status(403).body(Page.empty());
    }
}
