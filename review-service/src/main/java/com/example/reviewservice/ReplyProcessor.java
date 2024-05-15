package com.example.reviewservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ReplyProcessor {
    private CompletableFuture<String> future;

    @KafkaListener(topics = "review-service-response-get-user-by-email-topic", groupId = "review-service")
    public void processUserResponse(String userJson) {
        future.complete(userJson);
    }

    public CompletableFuture<String> waitForReply() {
        future = new CompletableFuture<>();
        return future;
    }
}