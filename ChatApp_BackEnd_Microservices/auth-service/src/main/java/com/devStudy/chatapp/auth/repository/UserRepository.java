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

    // 更新用户状态
    @Modifying
    @Query("update User u set u.active = ?2 where u.mail = ?1")
    void updateActive(String userEmail, boolean status);

    // 更新失败登录次数
    @Modifying
    @Query("update User u set u.failedAttempts = ?2 where u.mail = ?1")
    void updateFailedAttempts(String userEmail, int failedAttempts);

    // 更新密码
    @Modifying
    @Query("update User u set u.pwd = ?2 where u.mail = ?1")
    void updatePwd(String userEmail, String pwd);
}