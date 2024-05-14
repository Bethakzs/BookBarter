package com.example.userservice.service;


import com.example.userservice.dao.UserDAO;
import com.example.userservice.dto.BookStatus;
import com.example.userservice.dto.request.BookDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.entity.User;
import com.example.userservice.kafka.ReplyProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.print.Book;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDAO userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReplyProcessor replyProcessor;

    @Transactional
    public Optional<User> findByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));
        return Optional.of(user);
    }

    public UserDTO findByEmailUserDTO(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));

        CompletableFuture<String> booksIdFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(email)
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-all-books-id")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "user-service-response-get-all-books-id")
                .setHeader("serviceName", "user-service")
                .build());
        String booksIds = booksIdFuture.join();
        booksIds = booksIds.substring(1, booksIds.length()-1);
        List<Integer> booksIdInt = new ArrayList<>();
        if (!booksIds.isEmpty()) {
            booksIdInt = Arrays.stream(booksIds.split(", ")).map(Integer::parseInt).toList();
        }

        List<BookDTO> books = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Integer bookId : booksIdInt) {
            CompletableFuture<String> bookFuture = replyProcessor.waitForReply();
            kafkaTemplate.send(MessageBuilder.withPayload(bookId.toString())
                    .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-book-by-id-topic")
                    .setHeader(KafkaHeaders.REPLY_TOPIC, "user-service-response-get-book-by-id-topic")
                    .setHeader("serviceName", "user-service")
                    .build());
            String bookJson = bookFuture.join();
            try {
                BookDTO book = mapper.readValue(bookJson, BookDTO.class);
                books.add(book);

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing book", e);
            }
        }

        Iterator<BookDTO> iterator = books.iterator();
        while (iterator.hasNext()) {
            BookDTO book = iterator.next();
            if (book.getStatus().equals(BookStatus.RESERVED) || book.getStatus().equals(BookStatus.SOLD)) {
                iterator.remove();
            }
        }
        return UserDTO.builder()
                .login(user.getLogin())
                .email(user.getEmail())
                .phone(user.getPhone())
                .rating(user.getRating())
                .image(user.getImage())
                .bucks(user.getBucks())
                .notifications(user.isNotifications())
                .books(books)
                .build();
    }

    public User findByEmailWithOutCheck(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.orElse(null);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    public void deleteUser(String email) {
        userRepository.deleteByEmail(email);
    }

    @Transactional
    public Optional<User> findByEmailForCheck(String email) {
        return userRepository.findByEmail(email);
    }

    public void addBucksToUser(String name, Long bucks) {
        User user = userRepository.findByEmail(name).get();
        user.setBucks(user.getBucks() + bucks);
        userRepository.save(user);
    }
}