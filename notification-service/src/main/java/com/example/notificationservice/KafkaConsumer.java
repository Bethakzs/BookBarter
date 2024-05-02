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
        String subject = "Book Barter";
        String text = "Thank you for buying a book from Book Barter! Right now waiting when book will be delivered to you and after this you need to confirm that you received it on our site. Enjoy reading!";

        notificationService.sendSimpleMessage(to, subject, text);
    }
}
