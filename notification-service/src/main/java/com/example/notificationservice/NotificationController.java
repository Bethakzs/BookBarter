package com.example.notificationservice;

import jakarta.transaction.Transactional;
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
