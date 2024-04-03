package com.example.authservice.service;

import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.dao.UserRepository;
import com.example.authservice.dto.UserRegistration;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final @Lazy PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(username);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        List<GrantedAuthority> authorities = user.get().getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(user.get().getEmail(), user.get().getPwd(), authorities);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    @Transactional
    public User findByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }

    public void deleteUser(String email) {
        userRepository.deleteByEmail(email);
    }

    @Transactional
    public User createUser(UserRegistration userRegistration) throws IOException {
        User user = User.builder()
                .login(userRegistration.getLogin())
                .email(userRegistration.getEmail())
                .phone(passwordEncoder.encode(userRegistration.getPwd()))
                .pwd(passwordEncoder.encode(userRegistration.getPwd()))
                .image(userRegistration.getImage().getBytes())
                .buck(5L)
                .rating(3.5)
                .roles(new HashSet<>(Collections.singletonList(Role.ROLE_USER)))
                .build();
        return userRepository.save(user);
    }


    @Transactional
    public Optional<User> findByEmailForCheck(String email) {
        return userRepository.findByEmail(email);
    }

    public void addProfileImage(MultipartFile file, String name) {
        User user = userRepository.findByEmail(name).get();
        try {
            user.setImage(file.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBucksToUser(String email, Long bucks) {
        User user = userRepository.findByEmail(email).get();
        user.setBuck(user.getBuck() + bucks);
        userRepository.save(user);
    }

    public Optional<User> findByIdForCheck(Long userId) {
        return userRepository.findById(userId);
    }
}
