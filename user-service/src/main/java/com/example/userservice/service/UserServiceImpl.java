package com.example.userservice.service;

import com.example.userservice.dao.UserDAO;
import com.example.userservice.dto.book.BookStatus;
import com.example.userservice.dto.request.BookDTO;
import com.example.userservice.dto.response.UserDTO;
import com.example.userservice.entity.User;
import com.example.userservice.kafka.ReplyProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDAO userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReplyProcessor replyProcessor;

    @Transactional
    public User findByEmail(String email) throws Exception {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));
    }

    private List<Integer> getBookIds(String email) {
        CompletableFuture<String> booksIdFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(email)
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-all-books-id")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "user-service-response-get-all-books-id")
                .setHeader("serviceName", "user-service")
                .build());
        String booksIds = booksIdFuture.join();
        booksIds = booksIds.substring(1, booksIds.length()-1);
        if (!booksIds.isEmpty()) {
            return Arrays.stream(booksIds.split(", ")).map(Integer::parseInt).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private BookDTO getBookById(Integer bookId) {
        CompletableFuture<String> bookFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(bookId.toString())
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-book-by-id-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "user-service-response-get-book-by-id-topic")
                .setHeader("serviceName", "user-service")
                .build());
        String bookJson = bookFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(bookJson, BookDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing book", e);
        }
    }

    public UserDTO findByEmailUserDTO(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));

        List<Integer> booksIdInt = getBookIds(email);

        List<BookDTO> books = booksIdInt.stream()
                .map(this::getBookById)
                .filter(book -> !book.getStatus().equals(BookStatus.RESERVED) && !book.getStatus().equals(BookStatus.SOLD))
                .collect(Collectors.toList());

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
    public User findByEmailForCheck(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void addBucksToUser(String name, Long bucks) {
        User user = userRepository.findByEmail(name).get();
        user.setBucks(user.getBucks() + bucks);
        userRepository.save(user);
    }
}
