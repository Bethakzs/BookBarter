package com.example.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class responseDTO {
    private Long id;
    private String title;
    private String login;
    private String phone;
    private Double rating;
    private int price;
}
