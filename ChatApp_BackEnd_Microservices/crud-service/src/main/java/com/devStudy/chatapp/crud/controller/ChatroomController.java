package com.devStudy.chatapp.crud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devStudy.chatapp.crud.dto.ChatroomDTO;
import com.devStudy.chatapp.crud.dto.ChatroomRequestDTO;
import com.devStudy.chatapp.crud.dto.ChatroomWithOwnerAndStatusDTO;
import com.devStudy.chatapp.crud.dto.ModifyChatroomDTO;
import com.devStudy.chatapp.crud.dto.ModifyChatroomRequestDTO;
import com.devStudy.chatapp.crud.dto.UserDTO;
import com.devStudy.chatapp.crud.service.ChatroomService;
import com.devStudy.chatapp.crud.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chatrooms")
public class ChatroomController {

	private final UserService userService;

	private final ChatroomService chatroomService;

    @Autowired
    public ChatroomController(UserService userService, ChatroomService chatroomService) {
        this.userService = userService;
        this.chatroomService = chatroomService;
    }

	@PostMapping("/create")
	public ResponseEntity<Boolean> createChatroom(
			@RequestBody ChatroomRequestDTO chatroomRequestDTO, 
			@RequestHeader("X-User-Id") String userIdHeader) {
		long userId = userService.getUserIdFromHeaders(userIdHeader);
		if (chatroomService.createChatroom(chatroomRequestDTO, userId)) {
			return ResponseEntity.ok(true);
		}
		return ResponseEntity.status(409).body(false);
	}

	@DeleteMapping("/{chatroomId}")
	public ResponseEntity<Boolean> deleteChatroom(
			@PathVariable long chatroomId, 
			@RequestHeader("X-User-Id") String userIdHeader) {
		long userId = userService.getUserIdFromHeaders(userIdHeader);
		if (chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)) {
			if (chatroomService.deleteChatRoom(chatroomId)) {
				return ResponseEntity.ok(true);
			}
			return ResponseEntity.status(409).body(false);
		}
		return ResponseEntity.status(403).body(false);
	}

	@GetMapping("/{chatroomId}")
	public ResponseEntity<ModifyChatroomDTO> getChatroomForModify(
			@PathVariable long chatroomId, 
			@RequestHeader("X-User-Id") String userIdHeader) {
		long userId = userService.getUserIdFromHeaders(userIdHeader);
		Optional<ModifyChatroomDTO> chatroom = chatroomService.findChatroom(chatroomId);
		return chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)?
				  chatroom.map(ResponseEntity::ok)
						.orElseGet(() -> ResponseEntity.status(404).body(new ModifyChatroomDTO()))
				: ResponseEntity.status(403).body(new ModifyChatroomDTO());
	}

	@GetMapping("/{chatroomId}/users/invited")
	public ResponseEntity<Page<UserDTO>> getUsersInvitedInChatroom(
			@PathVariable long chatroomId,
			@RequestParam(defaultValue = "0") int page, 
			@RequestHeader("X-User-Id") String userIdHeader) {
		long userId = userService.getUserIdFromHeaders(userIdHeader);
		if (chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)) {
			return ResponseEntity.ok(userService.findUsersInvitedToChatroomByPage(chatroomId, page));
		}
		return ResponseEntity.status(403).body(Page.empty());
	}

	@GetMapping("/{chatroomId}/users/not-invited")
	public ResponseEntity<Page<UserDTO>> getUsersNotInvitedInChatroom(
			@PathVariable long chatroomId,
			@RequestParam(defaultValue = "0") int page, 
			@RequestHeader("X-User-Id") String userIdHeader) {
		long userId = userService.getUserIdFromHeaders(userIdHeader);
		if (chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)) {
			return ResponseEntity.ok(userService.findUsersNotInvitedToChatroomByPage(chatroomId, userId, page));
		}
		return ResponseEntity.status(403).body(Page.empty());
	}

	@PutMapping("/{chatroomId}")
	public ResponseEntity<Boolean> updateChatroomDetails(
			@PathVariable long chatroomId,
			@RequestBody ModifyChatroomRequestDTO chatroomRequest, 
			@RequestHeader("X-User-Id") String userIdHeader) {
		long userId = userService.getUserIdFromHeaders(userIdHeader);
		if (chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)) {
			if (chatroomService.updateChatroom(chatroomRequest, chatroomId)) {
				return ResponseEntity.ok(true);
			}
			return ResponseEntity.status(409).body(false);
		}
		return ResponseEntity.status(403).body(false);
	}

	@DeleteMapping("/{chatroomId}/users/invited/{userId}")
	public ResponseEntity<Boolean> leaveChatroom(
			@PathVariable long chatroomId, 
			@PathVariable long userId, 
			@RequestHeader("X-User-Id") String userIdHeader) {
		long currentUserId = userService.getUserIdFromHeaders(userIdHeader);
		if(userId == currentUserId) {
			if (chatroomService.deleteUserInvited(chatroomId, userId)) {
				return ResponseEntity.ok(true);
			} else {
				return ResponseEntity.status(500).body(false);
			}
		}
		return ResponseEntity.status(403).body(false);		
	}

	@GetMapping("/{chatroomId}/members")
	public ResponseEntity<List<UserDTO>> getAllMembersInChatroom(@PathVariable long chatroomId) {
		List<UserDTO> users = chatroomService.getAllUsersInChatroom(chatroomId);
		if (!users.isEmpty()) {
			return ResponseEntity.ok(users);
		}
		return ResponseEntity.status(500).body(new ArrayList<>());
	}
	
	// User-related endpoints integrated into this controller
	@GetMapping("/users/{userId}/owned")
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
    
    @GetMapping("/users/{userId}/joined")
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