package com.example.purchaseservice;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ReplyProcessor {
    private CompletableFuture<String> future;

    @KafkaListener(topics = "purchase-service-response-get-user-by-email-topic", groupId = "purchase-service")
    public void processUserResponse(String userJson) {
        future.complete(userJson);
    }

    @KafkaListener(topics = "purchase-service-response-get-book-by-id-topic", groupId = "purchase-service")
    public void processBookResponse(String bookJson) {
        future.complete(bookJson);
    }

    public CompletableFuture<String> waitForReply() {
        future = new CompletableFuture<>();
        return future;
    }
}