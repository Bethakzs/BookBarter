package com.example.purchaseservice.dto.secondly;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String login;
    private String email;
    private String pwd;
    private String phone;
    private Double rating;
    private byte[] image;
    private Long bucks;
}