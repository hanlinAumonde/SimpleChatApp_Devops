package com.devStudy.chat.websocket;

import static com.devStudy.chat.service.utils.ConstantValues.MESSAGE_ADD_CHATROOM_MEMBER;
import static com.devStudy.chat.service.utils.ConstantValues.MESSAGE_CONNECT;
import static com.devStudy.chat.service.utils.ConstantValues.MESSAGE_DISCONNECT;
import static com.devStudy.chat.service.utils.ConstantValues.MESSAGE_REMOVE_CHATROOM;
import static com.devStudy.chat.service.utils.ConstantValues.MESSAGE_REMOVE_CHATROOM_MEMBER;
import static com.devStudy.chat.service.utils.ConstantValues.MESSAGE_TEXT;
import static com.devStudy.chat.service.utils.ConstantValues.TO_ALL_IN_CHATROOM;
import static com.devStudy.chat.service.utils.ConstantValues.TO_OTHERS_IN_CHATROOM;
import static com.devStudy.chat.service.utils.ConstantValues.TO_SELF_IN_CHATROOM;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.devStudy.chat.dto.DTOMapper;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.model.User;
import com.devStudy.chat.service.implementations.ChatMessageService;
import com.devStudy.chat.service.implementations.UserService;
import com.devStudy.chat.service.utils.Events.ChangeChatroomMemberEvent;
import com.devStudy.chat.service.utils.Events.RemoveChatroomEvent;
import com.devStudy.chat.service.utils.Exceptions.WebSocketException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
	
	private static final Map<Long, ConcurrentHashMap<WebSocketSession, UserDTO>> CHATROOMS_MAP = new ConcurrentHashMap<>();
	
	private static final ObjectMapper MAPPER =  new ObjectMapper();
	
	private final UserService userService;
	private final ChatMessageService chatMessageService;

	@Autowired
	public ChatWebSocketHandler(UserService userService, ChatMessageService chatMessageService) {
		this.userService = userService;
		this.chatMessageService = chatMessageService;
	}

	// Voici les methodes utiles pour la gestion des messages websocket
	private UserDTO getUserInfo(long userId){
		try {
			Optional<User> user = userService.findUserById(userId);
	        return user.map(DTOMapper::toUserDTO).orElseThrow(() -> new WebSocketException("User not found"));
		} catch (Exception e) {
			throw new WebSocketException("Failed to get user info", e);
		}
    }
	
	private String setMessage(int messageType, String message, UserDTO userInfo, Date now) {
        //format : {user: {id: 1, username: "user1 user1"}, messageType: 0, message: "hello" , timestamp : "18:00"}
        try {
            //Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            ObjectNode finalNode = MAPPER.createObjectNode();

            ObjectNode userNode = MAPPER.createObjectNode();
            userNode.put("id", userInfo.getId());
            userNode.put("username", userInfo.getLastName() + " " + userInfo.getFirstName());

            finalNode.set("user", userNode);
            finalNode.put("messageType", messageType);
            finalNode.put("message", message);
            finalNode.put("timestamp", sdf.format(now));

            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(finalNode);
        } catch (JsonProcessingException e) {
            logger.error("Error while creating message : ", e);
            throw new WebSocketException("Error while creating message", e);
        }
    }
	
	private void sendMessageToSession(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
            	TextMessage textMessage = new TextMessage(message);
                session.sendMessage(textMessage);
            }
        } catch (IOException e) {
            throw new WebSocketException("Error while sending message to session", e);
        }
    }
	
	private void broadcastMessage(String message, long chatroomId, String broadcastType, WebSocketSession sender) {
        if (!message.isEmpty() && CHATROOMS_MAP.containsKey(chatroomId)) {
        	CHATROOMS_MAP.get(chatroomId).forEach((session, user) -> {
        		switch (broadcastType) {
					case TO_ALL_IN_CHATROOM:
						sendMessageToSession(session, message);
						break;
					case TO_SELF_IN_CHATROOM:
						if (session.equals(sender)) {
							sendMessageToSession(session, message);
						}
						break;
					case TO_OTHERS_IN_CHATROOM:
						if (!session.equals(sender)) {
							sendMessageToSession(session, message);
						}
						break;
					default:
						break;
				}
            });
        }
    }
	
	private void closeSessionQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            logger.error("Error while closing session", e);
        }
    }
	
	private void removeSessionFromChatrooms(WebSocketSession session) {
		long chatroomId = (long) session.getAttributes().get("chatroomId");
		// on retire la session de l'utilisateur de la chatroom
		if (CHATROOMS_MAP.containsKey(chatroomId)) {
			CHATROOMS_MAP.get(chatroomId).remove(session);
			if (CHATROOMS_MAP.get(chatroomId).isEmpty()) {
				CHATROOMS_MAP.remove(chatroomId);
				logger.info("Chatroom {} is empty now, removing it from the map", chatroomId);
			}
		}
    }
	
	
	// Voici les methodes utiles pour la gestion des connexions websocket
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		long chatroomId = (long) session.getAttributes().get("chatroomId");
		long userId = (long) session.getAttributes().get("userId");
		
		UserDTO userInfo = getUserInfo(userId);
		
		//on ajoute la session de l'utilisateur dans la chatroom correspondante, si la chatroom n'existe pas, on la crée
		if(CHATROOMS_MAP.containsKey(chatroomId)) {
			CHATROOMS_MAP.get(chatroomId).put(session, userInfo);
        } else {
            ConcurrentHashMap<WebSocketSession, UserDTO> chatroom = new ConcurrentHashMap<>();
            chatroom.put(session, userInfo);
            CHATROOMS_MAP.put(chatroomId, chatroom);
        }
		
		//on envoie un message de bienvenue à l'utilisateur
		Date date = new Date();
		broadcastMessage(
			setMessage(MESSAGE_CONNECT, TO_ALL_IN_CHATROOM, userInfo, date),
			chatroomId,
			TO_ALL_IN_CHATROOM,
			session
		);
		
        //Pour chaque utilisateur qui est déjà connecté dans le chatroom, on envoie un message de connexion à l'utilisateur qui ouvre la connexion
		for (Map.Entry<WebSocketSession, UserDTO> entry : CHATROOMS_MAP.get(chatroomId).entrySet()) {
			if (!entry.getKey().equals(session)) {
				broadcastMessage(
					setMessage(MESSAGE_CONNECT, TO_SELF_IN_CHATROOM, entry.getValue(), date),
					chatroomId,
					TO_SELF_IN_CHATROOM,
					session
				);
			}
		}
		
        logger.info("Connection opened for user {} in chatroom {}", userInfo.getLastName()+" "+userInfo.getFirstName(), chatroomId);
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
		UserDTO userInfo = getUserInfo(userId);

		// on envoie un message de déconnexion à tous les utilisateurs connect
		broadcastMessage(
			setMessage(MESSAGE_DISCONNECT, TO_OTHERS_IN_CHATROOM, userInfo, new Date()),
			chatroomId,
			TO_OTHERS_IN_CHATROOM,
			session
		);
		
		removeSessionFromChatrooms(session);
	}
	
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {
		long chatroomId = (long) session.getAttributes().get("chatroomId");
		long userId = (long) session.getAttributes().get("userId");
		UserDTO userInfo = getUserInfo(userId);
		
		String msg = message.getPayload();

		Date date = new Date();
		chatMessageService.saveMsgIntoCollection(chatroomId, userInfo, msg, date);
		//on envoie le message à tous les utilisateurs connectés
		broadcastMessage(
			setMessage(MESSAGE_TEXT, msg, userInfo, date), 
			chatroomId, 
			TO_ALL_IN_CHATROOM, 
			session
		);
	}
	
	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		logger.error("Error occurred in session: ", exception);
		closeSessionQuietly(session);
		removeSessionFromChatrooms(session);
	}
	
	// Voici les methodes utiles pour la gestion des evenements
	
	/*
     * cette methode surveille l'evenenment de suppression des chatrooms
     */
    //@EventListener
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void removeEventListener(RemoveChatroomEvent event) {
		long chatroomId = event.getEventMsg();
    	broadcastMessage(
    		setMessage(MESSAGE_REMOVE_CHATROOM,"This chatroom has been removed!",new UserDTO(),new Date()),
    		chatroomId,
    		TO_ALL_IN_CHATROOM,
    		null
    	);
        CHATROOMS_MAP.remove(chatroomId);
    }
	
	/*
	 * cette methode surveille l'evenenment de changement des membres des chatrooms
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void changeChatroomMemberEventListener(ChangeChatroomMemberEvent event) {
		long chatroomId = event.getChatroomId();
		for(UserDTO user : event.getAddedMembers()) {
			broadcastMessage(
				setMessage(MESSAGE_ADD_CHATROOM_MEMBER,"A new user has joined the chatroom!", user, new Date()),
				chatroomId,
				TO_ALL_IN_CHATROOM,
				null
			);
		}
		for(UserDTO user : event.getRemovedMembers()) {
			broadcastMessage(
				setMessage(MESSAGE_REMOVE_CHATROOM_MEMBER, "A user has left the chatroom!", user, new Date()),
				chatroomId,
				TO_ALL_IN_CHATROOM,
				null
			);
		}
    }
}
