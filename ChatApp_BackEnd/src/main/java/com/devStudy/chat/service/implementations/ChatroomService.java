package com.devStudy.chat.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devStudy.chat.dao.ChatroomRepository;
import com.devStudy.chat.dao.UserRepository;
import com.devStudy.chat.dto.ChatroomDTO;
import com.devStudy.chat.dto.ChatroomRequestDTO;
import com.devStudy.chat.dto.ChatroomWithOwnerAndStatusDTO;
import com.devStudy.chat.dto.DTOMapper;
import com.devStudy.chat.dto.ModifyChatroomDTO;
import com.devStudy.chat.dto.ModifyChatroomRequestDTO;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.model.Chatroom;
import com.devStudy.chat.model.User;
import com.devStudy.chat.service.interfaces.ChatroomServiceInt;
import com.devStudy.chat.service.utils.Events.ChangeChatroomMemberEvent;
import com.devStudy.chat.service.utils.Events.RemoveChatroomEvent;

import static com.devStudy.chat.service.utils.ConstantValues.DefaultPageSize_Chatrooms;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ChatroomService implements ChatroomServiceInt {

    private final Logger logger = LoggerFactory.getLogger(ChatroomService.class);

    private final UserRepository userRepository;
    private final ChatroomRepository chatroomRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public ChatroomService(UserRepository userRepository, ChatroomRepository chatroomRepository, ApplicationEventPublisher publisher) {
        this.userRepository = userRepository;
        this.chatroomRepository = chatroomRepository;
        this.publisher = publisher;
    }

    /**
     * Cette méthode permet de trouver le chatroom correspondant à l'id passé en paramètre
     */
    @Override
    public Optional<ModifyChatroomDTO> findChatroom(long chatroomId) {
    	return chatroomRepository.findById(chatroomId).map(DTOMapper::toModifyChatroomDTO);
    }

    /**
     * Cette méthode permet de créer un chatroom,
     * si il y a des conflits avec un chatroom existant, on retourne un chatroom vide et affichier un message d'erreur
     */
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
            
            // etape 1 : ajouter le créateur du chatroom
            chatroom.setCreator(creator);
            creator.getCreatedRooms().add(chatroom);
            
            // etape 2 : ajouter les utilisateurs invités
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

    /**
     * Cette méthode permet de trouver les chatrooms crées/joints par un utilisateur en Page(size = 5)
     */
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

    /**
     * Cette méthode permet de trouver les chatrooms crées/joints par un utilisateur
     */
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

    /**
     * Cette méthode permet de supprimer un chatroom
     */
    @Transactional
    @Override
    public boolean deleteChatRoom(long chatroomId) {
        try{
        	chatroomRepository.findById(chatroomId).ifPresent(
        		chatroom -> {
        			// supprimer les users invités
        			chatroom.getMembers().forEach(
	        			user -> {
	        				chatroom.getMembers().remove(user);
	        				user.getJoinedRooms().remove(chatroom);
	        			}
        			);
        			// pour le créateur, on supprime le chatroom depuis sa liste de chatrooms créés
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

    /**
     * Cette méthode permet de changer le status d'un chatroom
     */
    @Transactional
    @Override
    public void setStatusOfChatroom(long chatroomId, boolean status) {
        chatroomRepository.findById(chatroomId).ifPresent(chatroom -> chatroomRepository.updateActive(chatroom.getId(), status));
    }

    /**
     * Cette méthode permet de uninivter un utilisateur d'un chatroom
     */
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

    /**
     * Cette méthode permet de mettre à jour un chatroom
     */
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
            // ajouter les utilisateurs invités
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
            
			// supprimer les utilisateurs invités
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

    /**
     * Cette méthode permet de vérifier si un utilisateur est propriétaire d'un chatroom
     */
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
