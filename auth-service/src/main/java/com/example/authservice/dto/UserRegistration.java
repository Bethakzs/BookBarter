package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserRegistration {
    private String login;
    private String email;
    private String pwd;
    private String phone;
    private MultipartFile image;
}