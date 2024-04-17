package com.example.userservice;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/add-profile-image")
    @Transactional
    public ResponseEntity<?> addProfileImage(@RequestParam MultipartFile file, Principal principal) {
        userService.addProfileImage(file, principal.getName());
        return ResponseEntity.ok("Image added");
    }

    @GetMapping("/get")
    @Transactional
    public ResponseEntity<?> getUser(@RequestHeader("Authorization") String authHeader) throws Exception {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return ResponseEntity.ok(userService.findByEmailUserDTO(email));
    }

    @DeleteMapping("/delete/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email) throws Exception {
        if(userService.findByEmail(email).isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
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