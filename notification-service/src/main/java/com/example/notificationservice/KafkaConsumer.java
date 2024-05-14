package com.example.notificationservice;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {

    private final NotificationService notificationService;
    private static Long ID = 0L;

    @KafkaListener(topics = "notification-service-request-add-request", groupId = "notification-service")
    public void handleUpdateBookRequest(String request) {
        System.out.println("Received added request: " + request);
        String[] parts = request.split(":");
        Notification notification = Notification.builder()
                .id(ID)
                .title(parts[0])
                .email(parts[1])
                .login(parts[2])
                .phone(parts[3])
                .rating(Double.valueOf(parts[4]))
                .price(Integer.parseInt(parts[5]))
                .build();
        notificationService.add(notification);
        ID++;
    }
}
