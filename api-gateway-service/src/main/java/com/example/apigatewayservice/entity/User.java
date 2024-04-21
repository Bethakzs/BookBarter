package com.example.apigatewayservice.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String login;
    private String email;
    private String pwd;
    private String phone;
    private Double rating;
    private byte[] image;
    private Long buck;
    private List<Role> roles;
    private List<Long> wishListBookIds;
    private String refreshToken;
}
