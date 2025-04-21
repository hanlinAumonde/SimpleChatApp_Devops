package com.devStudy.chat.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.devStudy.chat.model.Chatroom;

import java.util.Optional;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {
    Optional<Chatroom> findById(long chatroomId);
    //Optional<Chatroom> findByTitreAndDescriptionAndHoraireCommenceAndHoraireTermine(String titre, String description, LocalDateTime horaireCommence, LocalDateTime horaireTermine);

    //Cette méthode permet de mise à jour le statut d'une chatroom
    @Modifying
    @Query("update Chatroom c set c.active = ?2 where c.id = ?1")
    void updateActive(long chatroomId, boolean status);

    //Cette méthode permet de trouver tous les chatrooms ouverts par un utilisateur, le chatroom doit être non expiré
    //@Query(value = "select c.* from chatrooms c, user_chatroom_relationship r where c.horaire_termine >= CURRENT_TIMESTAMP and c.id = r.chatroom_id and r.user_id = ?1", nativeQuery = true)
    @Query("SELECT c FROM Chatroom c JOIN c.members u WHERE u.id = ?1 AND c.horaireTermine >= CURRENT_TIMESTAMP")
    Page<Chatroom> findChatroomsJoinedOfUserByPage(long userId, Pageable pageable);
    
    //Cette méthode permet de trouver tous les chatrooms créés par un utilisateur, le chatroom doit être non expiré
    //@Query(value = "select c.* from chatrooms c where c.horaire_termine >= CURRENT_TIMESTAMP and c.creator_id = ?1", nativeQuery = true)
    @Query("SELECT c FROM Chatroom c JOIN c.creator u WHERE u.id = ?1 AND c.horaireTermine >= CURRENT_TIMESTAMP")
    Page<Chatroom> findChatroomsCreatedByUserByPage(long userId, Pageable pageable);
	
    @Query("SELECT c FROM Chatroom c JOIN c.creator u WHERE c.id = ?1 AND u.id = ?2")
    Optional<Chatroom> findByIdAndCreatorId(long chatroomId, long userId);
}
