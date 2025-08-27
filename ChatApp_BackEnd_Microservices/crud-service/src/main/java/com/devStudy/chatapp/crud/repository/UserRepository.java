package com.devStudy.chatapp.crud.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.devStudy.chatapp.crud.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("select u from User u where u.id <> ?1 and u.admin = false")
    Page<User> findAllOtherUsersNotAdminByPage(long userId, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.joinedRooms c WHERE c.id = ?1 AND u.admin = false")
    Page<User> findUsersInvitedToChatroomByPage(long chatroomId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id <> ?2 AND u.admin = false AND u.id NOT IN (SELECT u.id FROM User u JOIN u.joinedRooms c WHERE c.id = ?1)")
    Page<User> findUsersNotInvitedToChatroomByPage(long chatroomId, long creatorId, Pageable pageable);
}