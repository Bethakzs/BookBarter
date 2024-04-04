package com.example.userservice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserRegistration {
    private String login;
    private String email;
    private String pwd;
    private String phone;
    private MultipartFile image;
}