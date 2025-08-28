package com.devStudy.chatapp.message.service.Implementation;

import static com.devStudy.chatapp.message.utils.ConstantValues.ContentTimeStampFormat;
import static com.devStudy.chatapp.message.utils.ConstantValues.DateSignFormat;
import static com.devStudy.chatapp.message.utils.ConstantValues.MSG_CONTENT;
import static com.devStudy.chatapp.message.utils.ConstantValues.MSG_DATE_SIGN;
import static com.devStudy.chatapp.message.utils.ConstantValues.MSG_LATEST_DATE_SIGN;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.devStudy.chatapp.message.service.Interface.IChatMessageService;
import org.apache.commons.lang3.time.DateUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.devStudy.chatapp.message.repository.ChatMessageRepository;
import com.devStudy.chatapp.message.dto.ChatMsgDTO;
import com.devStudy.chatapp.message.dto.UserDTO;
import com.devStudy.chatapp.message.model.ChatMessage;

@Service
public class ChatMessageService implements IChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    @Value("${chatroomApp.pageable.DefaultPageSize_Messages}")
    private int DefaultPageSize_Messages;

    @Autowired
    public ChatMessageService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }
    
    private Pageable getPageableSetting(int page) {
        return PageRequest.of(page, DefaultPageSize_Messages, Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    @Override
    public void saveMsgIntoCollection(long chatroomId, UserDTO sender, String content, Date timestamp) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChatroomId(chatroomId);
        chatMessage.setUser(sender);
        chatMessage.setContent(content);
        chatMessage.setTimestamp(timestamp);
        chatMessageRepository.insert(chatMessage);
    }

    @Override
    public List<ChatMsgDTO> getChatMessagesByChatroomId(long chatroomId) {
        List<ChatMessage> initialRes = chatMessageRepository.findByChatroomId(chatroomId);
        return setResMsgList(initialRes, null);
    }
    
    @Override
    public List<ChatMsgDTO> getChatMessagesByChatroomIdByPage(long chatroomId, int page){
        List<ChatMessage> initialRes = chatMessageRepository.findByChatroomId(chatroomId, 
                getPageableSetting(page)).getContent();
        return setResMsgList(initialRes, null);
    }
    
    @Override
    public List<ChatMsgDTO> getChatMessagesByChatroomIdByPage(long chatroomId, int page, long currentUserId){
        List<ChatMessage> initialRes = chatMessageRepository.findByChatroomId(chatroomId, 
                getPageableSetting(page)).getContent();
        return setResMsgList(initialRes, currentUserId);
    }
    
    private List<ChatMsgDTO> setResMsgList(List<ChatMessage> initialList, Long currentUserId){
        List<ChatMsgDTO> res = new ArrayList<>();
        ChatMsgDTO latestDateSign = new ChatMsgDTO();
        int currentIndex = 0;
        for(int i = initialList.size()-1; i >= 0 ; i--) { 
             ChatMessage msg = initialList.get(i); 
             if(i == initialList.size()-1 || !(DateUtils.isSameDay(msg.getTimestamp(),initialList.get(i + 1).getTimestamp()))) { 
                 ChatMsgDTO dateSign = setDateSignMsg(currentIndex, DateSignFormat.format(msg.getTimestamp()));
                 latestDateSign.setMessageType(MSG_LATEST_DATE_SIGN);
                 latestDateSign.setTimestamp(dateSign.getTimestamp());
                 res.add(dateSign); 
                 currentIndex++; 
            }
            res.add(setContentMsg(currentIndex, msg, currentUserId)); 
            currentIndex++; 
        }
        if(!initialList.isEmpty()) {
            latestDateSign.setIndex(currentIndex);
            res.add(latestDateSign);
        }
        return res;
    }
    
    private ChatMsgDTO setDateSignMsg(int index, String date) {
        ChatMsgDTO msgDTO = new ChatMsgDTO();
        msgDTO.setIndex(index);
        msgDTO.setTimestamp(date);
        msgDTO.setMessageType(MSG_DATE_SIGN);
        return msgDTO;
    }
    
    private ChatMsgDTO setContentMsg(int index, ChatMessage msg, Long currentUserId) {
        ChatMsgDTO msgDTO = new ChatMsgDTO();
        msgDTO.setIndex(index);
        msgDTO.setUserId(msg.getUser().getId());
        msgDTO.setUsername(msg.getUser().getFirstName() + " " + msg.getUser().getLastName());
        msgDTO.setMessage(msg.getContent());
        msgDTO.setTimestamp(ContentTimeStampFormat.format(msg.getTimestamp()));
        
        // 设置是否是当前用户发送的消息
        if (currentUserId != null) {
            msgDTO.setSentByUser(msg.getUser().getId() == currentUserId);
        } else {
            msgDTO.setSentByUser(false);
        }
        msgDTO.setMessageType(MSG_CONTENT);
        return msgDTO;
    }
}