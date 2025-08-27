package com.devStudy.chatapp.crud.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.devStudy.chatapp.crud.model.Chatroom;

import java.util.Optional;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {
    Optional<Chatroom> findById(long chatroomId);

    @Modifying
    @Query("update Chatroom c set c.active = ?2 where c.id = ?1")
    void updateActive(long chatroomId, boolean status);

    @Query("SELECT c FROM Chatroom c JOIN c.members u WHERE u.id = ?1 AND c.horaireTermine >= CURRENT_TIMESTAMP")
    Page<Chatroom> findChatroomsJoinedOfUserByPage(long userId, Pageable pageable);
    
    @Query("SELECT c FROM Chatroom c JOIN c.creator u WHERE u.id = ?1 AND c.horaireTermine >= CURRENT_TIMESTAMP")
    Page<Chatroom> findChatroomsCreatedByUserByPage(long userId, Pageable pageable);
	
    @Query("SELECT c FROM Chatroom c JOIN c.creator u WHERE c.id = ?1 AND u.id = ?2")
    Optional<Chatroom> findByIdAndCreatorId(long chatroomId, long userId);
}