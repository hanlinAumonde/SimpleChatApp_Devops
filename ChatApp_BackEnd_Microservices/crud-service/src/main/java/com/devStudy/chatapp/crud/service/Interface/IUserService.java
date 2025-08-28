package com.devStudy.chatapp.crud.service.Interface;

import com.devStudy.chatapp.crud.dto.UserDTO;
import org.springframework.data.domain.Page;

public interface IUserService {
    long getUserIdFromHeaders(String userIdHeader);
    Page<UserDTO> findAllOtherUsersNotAdminByPage(int page, long userId);
    Page<UserDTO> findUsersNotInvitedToChatroomByPage(long chatroomId, long userId, int page);
}
