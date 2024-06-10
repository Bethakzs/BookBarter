package com.example.userservice.controller;

import com.example.userservice.util.JwtTokenProvider;
import com.example.userservice.service.UserServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/get")
    @Transactional
    public ResponseEntity<?> getUser(@RequestHeader("Authorization") String authHeader) throws Exception {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return ResponseEntity.ok(userService.findByEmailUserDTO(email));
    }

    @DeleteMapping("/delete/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email) throws Exception {
        userService.findByEmail(email);
        userService.deleteUser(email);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/add-buck/{bucks}")
    @Transactional
    public ResponseEntity<?> addBucksToUser(@PathVariable Long bucks, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        userService.addBucksToUser(email, bucks);
        return ResponseEntity.ok("bucks added");
    }
}
