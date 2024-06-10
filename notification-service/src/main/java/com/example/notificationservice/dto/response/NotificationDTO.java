package com.example.notificationservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String title;
    private String login;
    private String phone;
    private Double rating;
    private int price;
}
