package com.devStudy.chatapp.crud.service.Interface;

import com.devStudy.chatapp.crud.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface IChatroomService {
    Optional<ModifyChatroomDTO> findChatroom(long chatroomId);
    boolean createChatroom(ChatroomRequestDTO chatroomRequestDTO, long userId);
    Page<ChatroomDTO> getChatroomsOwnedOfUserByPage(long userId, int page);
    Page<ChatroomWithOwnerAndStatusDTO> getChatroomsJoinedOfUserByPage(long userId, boolean isOwner, int page);
    List<UserDTO> getAllUsersInChatroom(long chatroomId);
    boolean deleteChatRoom(long chatroomId);
    void setStatusOfChatroom(long chatroomId, boolean status);
    boolean deleteUserInvited(long chatroomId, long userId);
    boolean updateChatroom(ModifyChatroomRequestDTO chatroomRequestDTO, long chatroomId);
    boolean checkUserIsOwnerOfChatroom(long userId, long chatroomId);
}
