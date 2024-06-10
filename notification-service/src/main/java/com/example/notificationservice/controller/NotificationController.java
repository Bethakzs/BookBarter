package com.example.notificationservice.controller;

import com.example.notificationservice.service.NotificationService;
import com.example.notificationservice.service.NotificationServiceImpl;
import com.example.notificationservice.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/get")
    public ResponseEntity<?> getAll(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return ResponseEntity.ok(notificationService.getAll(email));
    }
}
