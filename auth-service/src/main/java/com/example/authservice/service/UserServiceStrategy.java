package com.example.authservice.service;

import com.example.authservice.dto.JwtRequest;
import org.springframework.http.ResponseEntity;

public interface UserServiceStrategy {

    ResponseEntity<?> execute(JwtRequest request);
}

