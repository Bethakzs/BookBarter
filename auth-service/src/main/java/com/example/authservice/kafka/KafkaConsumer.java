package com.example.authservice.kafka;

import com.example.authservice.entity.User;
import com.example.authservice.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class KafkaConsumer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private UserService userService;

    @KafkaListener(topics = "user-service-request-check-exist-user-topic", groupId = "user-service")
    public void processRequestCheck(ConsumerRecord<String, String> request, @Header(KafkaHeaders.REPLY_TOPIC) String replyTopic) {
        Long userId = Long.parseLong(request.value());
        Optional<User> book = userService.findByIdForCheck(userId);
        String exists = book.isPresent() ? "true" : "false";
        kafkaTemplate.send(MessageBuilder.withPayload(exists)
                .setHeader(KafkaHeaders.TOPIC, replyTopic)
                .setHeader("serviceName", "user-service")
                .build());
    }

    @Transactional
    @KafkaListener(topics = "user-service-request-get-user-by-id-topic", groupId = "user-service")
    public void processRequestGetBook(ConsumerRecord<String, String> request, @Header(KafkaHeaders.REPLY_TOPIC) String replyTopic) {
        Long userId = Long.parseLong(request.value());
        Optional<User> userOptional = userService.findByIdForCheck(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            ObjectMapper objectMapper = new ObjectMapper();
            System.out.println("Sending user: " + replyTopic);
            try {
                String bookJson = objectMapper.writeValueAsString(user);
                kafkaTemplate.send(replyTopic, bookJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing JSON", e);
            }
        } else {
            kafkaTemplate.send(replyTopic, "null");
        }
    }
}
