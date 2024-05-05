package com.example.authservice.service;

import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.dao.UserRepository;
import com.example.authservice.dto.UserRegistration;
import com.example.authservice.kafka.ReplyProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.mapper.Mapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final @Lazy PasswordEncoder passwordEncoder;
    private final ReplyProcessor replyProcessor;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CompletableFuture<String> userFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(username)
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-without-exist-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "auth-service-response-get-user-by-email-topic")
                .setHeader("serviceName", "auth-service")
                .build());
        String userJson = userFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        User user;
        try {
            user = mapper.readValue(userJson, User.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing user", e);
        }
        if(user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPwd(), authorities);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    @Transactional
    public User findByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }

    @Transactional
    public User createUser(UserRegistration userRegistration) {
        User user = User.builder()
                .login(userRegistration.getLogin())
                .email(userRegistration.getEmail())
                .phone(userRegistration.getPhone())
                .pwd(passwordEncoder.encode(userRegistration.getPwd()))
                .image(null)
                .bucks(5L)
                .rating(3.5)
                .roles(new HashSet<>(Collections.singletonList(Role.ROLE_USER)))
                .build();
        if(userRegistration.getImage() != null) {
            try {
                user.setImage(userRegistration.getImage().getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        kafkaTemplate.send(MessageBuilder.withPayload(user.getEmail())
                .setHeader(KafkaHeaders.TOPIC, "notification-service-request-register")
                .setHeader("serviceName", "notification-service")
                .build());
        return userRepository.save(user);
    }

    public Optional<User> findByIdForCheck(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional
    public Optional<User> findByEmailForCheck(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public Optional<User> findByLoginForCheck(String login) {
        return userRepository.findByLogin(login);
    }

    @Transactional
    public Optional<User> findByPhoneForCheck(String phone) {
        return userRepository.findByPhone(phone);
    }
}
