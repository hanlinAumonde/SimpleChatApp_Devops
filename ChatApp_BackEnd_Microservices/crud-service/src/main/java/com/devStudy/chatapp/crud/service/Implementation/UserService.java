package com.devStudy.chatapp.crud.service.Implementation;

import com.devStudy.chatapp.crud.service.Interface.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devStudy.chatapp.crud.dto.DTOMapper;
import com.devStudy.chatapp.crud.dto.UserDTO;
import com.devStudy.chatapp.crud.model.User;
import com.devStudy.chatapp.crud.repository.UserRepository;

@Service
public class UserService implements IUserService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Value("${chatroomApp.pageable.DefaultPageSize_Users}")
    private int DefaultPageSize_Users;

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private Pageable getPageableSetting(int page) {
        return PageRequest.of(page, DefaultPageSize_Users, Sort.sort(User.class).by(User::getFirstName).ascending());
    }

    @Override
    public long getUserIdFromHeaders(String userIdHeader) {
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid user ID in header: {}", userIdHeader);
            throw new IllegalArgumentException("Invalid user ID");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserDTO> findAllOtherUsersNotAdminByPage(int page, long userId) {
        return userRepository.findAllOtherUsersNotAdminByPage(userId, this.getPageableSetting(page))
                .map(DTOMapper::toUserDTO);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> findUsersInvitedToChatroomByPage(long chatroomId, int page) {
        return userRepository.findUsersInvitedToChatroomByPage(chatroomId, this.getPageableSetting(page))
                .map(DTOMapper::toUserDTO);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserDTO> findUsersNotInvitedToChatroomByPage(long chatroomId, long userId, int page) {
        return userRepository.findUsersNotInvitedToChatroomByPage(chatroomId, userId, this.getPageableSetting(page))
                .map(DTOMapper::toUserDTO);
    }
}