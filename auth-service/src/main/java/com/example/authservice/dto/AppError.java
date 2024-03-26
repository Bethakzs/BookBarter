package com.example.authservice.dto;

import lombok.Data;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Data
public class AppError {
    private int status;
    private String message;
    private Date timestamp;

    public AppError(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = new Date();
    }
}
