package com.example.notificationservice.service;

import com.example.notificationservice.dao.NotificationRepository;
import com.example.notificationservice.dto.response.NotificationDTO;
import com.example.notificationservice.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public List<NotificationDTO> getAll(String email) {
        List<NotificationDTO> notifications = notificationRepository.findAllByEmail(email);
        kafkaTemplate.send(MessageBuilder.withPayload(email)
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-change-notification-false")
                .setHeader("serviceName", "user-service")
                .build());
        return notifications;
    }

    public void add(Notification notification) {
        notificationRepository.save(notification);
    }
}
