package com.devStudy.chat.dao;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.devStudy.chat.model.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, ObjectId> {
	@Query(sort = "{ timestamp : 1 }")
	List<ChatMessage> findByChatroomId(long chatroomId);
	
	Page<ChatMessage> findByChatroomId(long chatroomId, Pageable pageable);
}
