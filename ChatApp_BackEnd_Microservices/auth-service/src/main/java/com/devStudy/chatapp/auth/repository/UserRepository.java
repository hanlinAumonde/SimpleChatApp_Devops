package com.devStudy.chatapp.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.devStudy.chatapp.auth.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMailAndAdmin(String mail, boolean isAdmin);

    // Update active status
    @Modifying
    @Query("update User u set u.active = ?2 where u.mail = ?1")
    void updateActive(String userEmail, boolean status);

    // Update failed attempts
    @Modifying
    @Query("update User u set u.failedAttempts = ?2 where u.mail = ?1")
    void updateFailedAttempts(String userEmail, int failedAttempts);

    // Update password
    @Modifying
    @Query("update User u set u.pwd = ?2 where u.mail = ?1")
    void updatePwd(String userEmail, String pwd);
}