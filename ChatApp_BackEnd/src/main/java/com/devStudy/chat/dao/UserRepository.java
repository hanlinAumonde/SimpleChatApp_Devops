package com.devStudy.chat.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.devStudy.chat.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMailAndAdmin(String mail, boolean isAdmin);

    Page<User> findByAdmin(boolean isAdmin, Pageable pageable);

    //Page<User> findByActive(boolean isActive, Pageable pageable);

    //Cette méthode permet de mise à jour le statut d'un utilisateur
    @Modifying
    @Query("update User u set u.active = ?2 where u.mail = ?1")
    void updateActive(String userEmail, boolean status);

    //Cette méthode permet de mise à jour le compteur d'essai de connexion d'un utilisateur
    @Modifying
    @Query("update User u set u.failedAttempts = ?2 where u.mail = ?1")
    void updateFailedAttempts(String userEmail, int failedAttempts);

    //Cette méthode permet de mise à jour le mot de passe d'un utilisateur
    @Modifying
    @Query("update User u set u.pwd = ?2 where u.mail = ?1")
    void updatePwd(String userEmail, String pwd);

    //Optional<User> findByMail(String email);

    //Cette méthode permet de trouver tous les utilisateurs qui ne sont pas administrateurs
    @Query("select u from User u where u.id <> ?1 and u.admin = false")
    Page<User> findAllOtherUsersNotAdminByPage(long userId, Pageable pageable);

    //Cette méthode permet de trouver tous les utilisateurs qui ne sont pas administrateurs et qui sont invités à un chatroom
    //@Query(value = "select u from users u, user_chatroom_relationship r where u.id = r.user_id and r.chatroom_id = ?1", nativeQuery = true)
    @Query("SELECT u FROM User u JOIN u.joinedRooms c WHERE c.id = ?1 AND u.admin = false")
    Page<User> findUsersInvitedToChatroomByPage(long chatroomId, Pageable pageable);

    //Cette méthode permet de trouver tous les utilisateurs qui ne sont pas administrateurs et qui ne sont pas invités à un chatroom
    @Query("SELECT u FROM User u WHERE u.id <> ?2 AND u.admin = false AND u.id NOT IN (SELECT u.id FROM User u JOIN u.joinedRooms c WHERE c.id = ?1)")
    Page<User> findUsersNotInvitedToChatroomByPage(long chatroomId, long creatorId, Pageable pageable);
}
