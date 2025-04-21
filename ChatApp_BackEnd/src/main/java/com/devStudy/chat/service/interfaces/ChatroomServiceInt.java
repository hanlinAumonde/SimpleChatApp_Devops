package com.devStudy.chat.service.interfaces;

import org.springframework.data.domain.Page;

import com.devStudy.chat.dto.ChatroomDTO;
import com.devStudy.chat.dto.ChatroomRequestDTO;
import com.devStudy.chat.dto.ChatroomWithOwnerAndStatusDTO;
import com.devStudy.chat.dto.ModifyChatroomDTO;
import com.devStudy.chat.dto.ModifyChatroomRequestDTO;
import com.devStudy.chat.dto.UserDTO;

import java.util.List;
import java.util.Optional;

public interface ChatroomServiceInt {
    Optional<ModifyChatroomDTO> findChatroom(long chatroomId);

    boolean createChatroom(ChatroomRequestDTO chatroomRequestDTO, long userId);

    Page<ChatroomDTO> getChatroomsOwnedOfUserByPage(long userId, int page);
    
    Page<ChatroomWithOwnerAndStatusDTO> getChatroomsJoinedOfUserByPage(long userId, boolean isOwner, int page);
    
    List<UserDTO> getAllUsersInChatroom(long chatroomId);

    boolean deleteChatRoom(long chatRoomId);

    void setStatusOfChatroom(long chatRoomId,boolean status);

    boolean deleteUserInvited(long chatroomId, long userId);

    boolean checkUserIsOwnerOfChatroom(long userId, long chatroomId);

	/**
	 * Cette méthode permet de mettre à jour un chatroom
	 */
	boolean updateChatroom(ModifyChatroomRequestDTO chatroomRequestDTO, long chatroomId);
}
