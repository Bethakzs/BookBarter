package com.example.authservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistration {
    private String login;
    private String email;
    private String pwd;
    private String phone;
}