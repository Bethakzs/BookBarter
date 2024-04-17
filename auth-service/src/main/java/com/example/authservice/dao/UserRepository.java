package com.example.authservice.dao;

import com.example.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);

    User findByRefreshToken(String refreshToken);

    Optional<User> findByLogin(String login);

    Optional<User> findByPhone(String phone);
}
