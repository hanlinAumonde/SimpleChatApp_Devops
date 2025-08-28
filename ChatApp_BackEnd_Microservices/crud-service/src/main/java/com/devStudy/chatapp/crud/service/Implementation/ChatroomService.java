package com.devStudy.chatapp.crud.service.Implementation;

import com.devStudy.chatapp.crud.service.Interface.IChatroomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devStudy.chatapp.crud.dto.ChatroomDTO;
import com.devStudy.chatapp.crud.dto.ChatroomRequestDTO;
import com.devStudy.chatapp.crud.dto.ChatroomWithOwnerAndStatusDTO;
import com.devStudy.chatapp.crud.dto.DTOMapper;
import com.devStudy.chatapp.crud.dto.ModifyChatroomDTO;
import com.devStudy.chatapp.crud.dto.ModifyChatroomRequestDTO;
import com.devStudy.chatapp.crud.dto.UserDTO;
import com.devStudy.chatapp.crud.events.ChangeChatroomMemberEvent;
import com.devStudy.chatapp.crud.events.RemoveChatroomEvent;
import com.devStudy.chatapp.crud.model.Chatroom;
import com.devStudy.chatapp.crud.model.User;
import com.devStudy.chatapp.crud.repository.ChatroomRepository;
import com.devStudy.chatapp.crud.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatroomService implements IChatroomService {

    private final Logger logger = LoggerFactory.getLogger(ChatroomService.class);

    @Value("${chatroomApp.pageable.DefaultPageSize_Chatrooms}")
    private int DefaultPageSize_Chatrooms;

    private final UserRepository userRepository;
    private final ChatroomRepository chatroomRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public ChatroomService(UserRepository userRepository, ChatroomRepository chatroomRepository, ApplicationEventPublisher publisher) {
        this.userRepository = userRepository;
        this.chatroomRepository = chatroomRepository;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ModifyChatroomDTO> findChatroom(long chatroomId) {
    	return chatroomRepository.findById(chatroomId).map(DTOMapper::toModifyChatroomDTO);
    }

    @Transactional
    @Override
    public boolean createChatroom(ChatroomRequestDTO chatroomRequestDTO, long userId) {
        try {
            Chatroom chatroom = new Chatroom();
            chatroom.setTitre(chatroomRequestDTO.getTitre());
            chatroom.setDescription(chatroomRequestDTO.getDescription());
            chatroom.setActive(true);

            LocalDateTime dateStart = LocalDateTime.parse(chatroomRequestDTO.getStartDate());
            LocalDateTime dateEnd = dateStart.plusDays(
                    chatroomRequestDTO.getDuration() > 0 ? chatroomRequestDTO.getDuration() : 1
            );
            chatroom.setHoraireCommence(dateStart);
            chatroom.setHoraireTermine(dateEnd);

            List<Chatroom> allChatrooms = chatroomRepository.findAll();
            for (Chatroom c : allChatrooms) {
                if (c.equals(chatroom)) {
                    return false;
                }
            }
            
            User creator = userRepository.findById(userId).orElseThrow();
            
            chatroom.setCreator(creator);
            creator.getCreatedRooms().add(chatroom);
            
            List<Long> invitedUserIds = chatroomRequestDTO.getUsersInvited().stream().map(UserDTO::getId).toList();
            for(var userInvited: userRepository.findAllById(invitedUserIds)) {
            	chatroom.getMembers().add(userInvited);
            	userInvited.getJoinedRooms().add(chatroom);
            }
            
            chatroomRepository.save(chatroom);
            return true;
        } catch (Exception e) {
            logger.error("Error while creating chatroom : {}", e.getMessage());
            return false;
        }
    }

    private Page<Chatroom> getChatroomsOwnedOrJoinedOfUserByPage(long userId, boolean isOwner, int page) {
        Pageable pageable = PageRequest.of(page, DefaultPageSize_Chatrooms, Sort.sort(Chatroom.class).by(Chatroom::getTitre).ascending());
        return isOwner? chatroomRepository.findChatroomsCreatedByUserByPage(userId, pageable) :
        				chatroomRepository.findChatroomsJoinedOfUserByPage(userId,pageable);
    }

    @Transactional(readOnly = true)
    @Override
	public Page<ChatroomDTO> getChatroomsOwnedOfUserByPage(long userId, int page) {
		Page<Chatroom> chatrooms = getChatroomsOwnedOrJoinedOfUserByPage(userId, true, page);
		return chatrooms.map(chatroom ->
                DTOMapper.toChatroomDTO(chatroom, chatroom.isActive() && !chatroom.hasNotStarted())
		);
	}

    @Transactional(readOnly = true)
    @Override
	public Page<ChatroomWithOwnerAndStatusDTO> getChatroomsJoinedOfUserByPage(long userId, boolean isOwner, int page) {
    	Page<Chatroom> chatrooms = getChatroomsOwnedOrJoinedOfUserByPage(userId, isOwner, page);
    	return chatrooms.map(chatroom -> {
    		boolean isActive = chatroom.isActive() && !chatroom.hasNotStarted();
    		return DTOMapper.toChatroomWithOwnerAndStatusDTO(chatroom, isActive);
    	});
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDTO> getAllUsersInChatroom(long chatroomId){
        List<User> allUsersInChatroom = new ArrayList<>();
        chatroomRepository.findById(chatroomId).ifPresent(
        	chatroom -> {
        		allUsersInChatroom.addAll(chatroom.getMembers());
        		allUsersInChatroom.add(chatroom.getCreator());
        	}
        );
        return allUsersInChatroom.stream().map(DTOMapper::toUserDTO).toList();
    }

    @Transactional
    @Override
    public boolean deleteChatRoom(long chatroomId) {
        try{
        	chatroomRepository.findById(chatroomId).ifPresent(
        		chatroom -> {
                    // Use an iterator to avoid ConcurrentModificationException
                    Iterator<User> userIterator = chatroom.getMembers().iterator();
                    while(userIterator.hasNext()) {
                    	User user = userIterator.next();
                    	userIterator.remove();
                    	user.getJoinedRooms().remove(chatroom);
                    }
        			chatroom.getCreator().getCreatedRooms().remove(chatroom);
        			chatroomRepository.delete(chatroom);
        		}
        	);
            publisher.publishEvent(new RemoveChatroomEvent(chatroomId));
            return true;
        }catch (Exception e){
            logger.error("Error while deleting chatroom with id {} : {}", chatroomId, e.getMessage());
            return false;
        }
    }

    @Transactional
    @Override
    public void setStatusOfChatroom(long chatroomId, boolean status) {
        chatroomRepository.findById(chatroomId).ifPresent(chatroom -> chatroomRepository.updateActive(chatroom.getId(), status));
    }

    @Transactional
    @Override
    public boolean deleteUserInvited(long chatroomId, long userId){
        try{
        	UserDTO user = new UserDTO();
        	Optional<Chatroom> chatroom = chatroomRepository.findById(chatroomId);
        	if(chatroom.isPresent()) {
        		for(var member: chatroom.get().getMembers()) {
    				if (member.getId() == userId) {
    					chatroom.get().getMembers().remove(member);
    					member.getJoinedRooms().remove(chatroom.get());
    					user = DTOMapper.toUserDTO(member);
    					break;
    				}
    			}
        	}else {
        		return false;
        	}
            publisher.publishEvent(new ChangeChatroomMemberEvent(chatroomId, List.of(), List.of(user)));
            return true;
        }catch (Exception e){
            logger.error("Error while deleting user with id {} from chatroom with id {} : {}", userId, chatroomId, e.getMessage());
            return false;
        }
    }

    @Transactional
    @Override
    public boolean updateChatroom(ModifyChatroomRequestDTO chatroomRequestDTO, long chatroomId) {
        try{
            Chatroom chatroom = chatroomRepository.findById(chatroomId).orElseThrow();
            boolean isChanged = false;
            if(!chatroom.getTitre().equals(chatroomRequestDTO.getTitre())){
                chatroom.setTitre(chatroomRequestDTO.getTitre());
                isChanged = true;
            }
            if(!chatroom.getDescription().equals(chatroomRequestDTO.getDescription())){
                chatroom.setDescription(chatroomRequestDTO.getDescription());
                isChanged = true;
            }
            if(!Objects.equals(chatroomRequestDTO.getStartDate(), "")){
                if(!chatroom.getHoraireCommence().equals(LocalDateTime.parse(chatroomRequestDTO.getStartDate()))){
                    chatroom.setHoraireCommence(LocalDateTime.parse(chatroomRequestDTO.getStartDate()));
                    isChanged = true;
                }
            }
            LocalDateTime dateEnd = chatroom.getHoraireCommence().plusDays(
                    chatroomRequestDTO.getDuration()
            );
            if(!chatroom.getHoraireTermine().equals(dateEnd)){
                chatroom.setHoraireTermine(dateEnd);
                isChanged = true;
            }
			for (UserDTO user : chatroomRequestDTO.getListAddedUsers()) {
                Optional<User> userOptional = userRepository.findById(user.getId());
				if(userOptional.isEmpty()) {
                    continue;
                }
                User userInvited = userOptional.get();
				if(!chatroom.getMembers().contains(userInvited)) {
					chatroom.getMembers().add(userInvited);
					userInvited.getJoinedRooms().add(chatroom);
					isChanged = true;
				}
			}
            
			for (UserDTO user : chatroomRequestDTO.getListRemovedUsers()) {
				Optional<User> userOptional = userRepository.findById(user.getId());
                if(userOptional.isEmpty()) {
                    continue;
                }
                User userRemoved = userOptional.get();
				if (chatroom.getMembers().contains(userRemoved) && !chatroom.getCreator().equals(userRemoved)) {
					chatroom.getMembers().remove(userRemoved);
					userRemoved.getJoinedRooms().remove(chatroom);
					isChanged = true;
				}
			}
			
            if(isChanged){
                chatroomRepository.save(chatroom);
            }
            
            publisher.publishEvent(
            	new ChangeChatroomMemberEvent(
            	  chatroomId,
				  chatroomRequestDTO.getListAddedUsers(),
				  chatroomRequestDTO.getListRemovedUsers()
		        )
            );
            return true;
        }catch (RuntimeException e){
            logger.error("Error while updating chatroom with id {} : {}", chatroomId, e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean checkUserIsOwnerOfChatroom(long userId, long chatroomId) {
        try {
            return chatroomRepository.findByIdAndCreatorId(chatroomId, userId).isPresent();
        } catch (RuntimeException e) {
            logger.error("Error while checking if user with id {} is owner of chatroom with id {} : {}", userId, chatroomId, e.getMessage());
            return false;
        }
    }
}