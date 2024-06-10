package com.example.userservice.service;

import com.example.userservice.dto.response.UserDTO;
import com.example.userservice.entity.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface UserService {

    User findByEmail(String email) throws Exception;

    UserDTO findByEmailUserDTO(String email) throws Exception;

    User findByEmailWithOutCheck(String email);

    void updateUser(User user);

    void deleteUser(String email);

    User findByEmailForCheck(String email);

    void addBucksToUser(String name, Long bucks);
}
