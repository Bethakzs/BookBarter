package com.example.userservice.kafka;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ReplyProcessor {
    private CompletableFuture<String> future;

    @KafkaListener(topics = "user-service-response-get-all-books-id", groupId = "user-service")
    public void processUserResponse(String userJson) {
        future.complete(userJson);
    }

    @KafkaListener(topics = "user-service-response-get-book-by-id-topic", groupId = "user-service")
    public void processBookResponse(String bookJson) {
        future.complete(bookJson);
    }

    public CompletableFuture<String> waitForReply() {
        future = new CompletableFuture<>();
        return future;
    }
}