package com.example.userservice.service;


import com.example.userservice.dao.UserDAO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDAO userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Optional<User> findByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));
        return Optional.of(user);
    }

    @Transactional
    public UserDTO findByEmailUserDTO(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));
        return UserDTO.builder()
                .id(user.getId())
                .login(user.getLogin())
                .email(user.getEmail())
                .phone(passwordEncoder.encode(user.getPhone()))
                .rating(user.getRating())
                .image(user.getImage())
                .bucks(user.getBucks())
                .build();
    }

    public User findByEmailWithOutCheck(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.orElse(null);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    public User findByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }

    public void deleteUser(String email) {
        userRepository.deleteByEmail(email);
    }

    @Transactional
    public Optional<User> findByEmailForCheck(String email) {
        return userRepository.findByEmail(email);
    }

    public void addProfileImage(MultipartFile file, String name) {
        User user = userRepository.findByEmail(name).get();
        try {
            user.setImage(file.getBytes());
            userRepository.save(user);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBucksToUser(String name, Long bucks) {
        User user = userRepository.findByEmail(name).get();
        user.setBucks(user.getBucks() + bucks);
        userRepository.save(user);
    }
}