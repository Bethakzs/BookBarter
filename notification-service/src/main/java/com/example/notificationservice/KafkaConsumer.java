package com.example.notificationservice;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "notification-service-request-send-buy-book-topic", groupId = "notification-service")
    public void processRequestCheck(ConsumerRecord<String, String> request) {
        String to = request.value();
        String text = "Thank you for buying";
        System.out.println("Sending email to: " + to);

        notificationService.sendSimpleMessage(to, text);
    }

    @KafkaListener(topics = "notification-service-request-register", groupId = "notification-service")
    public void processRequestRegister(ConsumerRecord<String, String> request) {
        String to = request.value();
        String text = "Thank you for registration";
        System.out.println("Sending email to: " + to);

        notificationService.sendSimpleMessage(to, text);
    }
}
