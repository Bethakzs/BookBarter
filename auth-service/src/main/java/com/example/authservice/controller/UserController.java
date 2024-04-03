package com.example.authservice.controller;

import com.example.authservice.entity.User;
import com.example.authservice.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/checkEmail/{email}")
    @Transactional
    public Optional<User> findByEmailForCheck(@PathVariable String email) {
        return userService.findByEmailForCheck(email);
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody User user) {
        userService.updateUser(user);
        return ResponseEntity.ok("User updated");
    }

    @PostMapping("/update-profile-image")
    @Transactional
    public ResponseEntity<?> addProfileImage(@RequestParam MultipartFile file, Principal principal) {
        userService.addProfileImage(file, principal.getName());
        return ResponseEntity.ok("Image added");
    }

    @GetMapping("/get-user/{email}")
    @Transactional
    public ResponseEntity<?> getUser(@PathVariable String email) {
        return ResponseEntity.ok(userService.findByEmailForCheck(email));
    }

    @DeleteMapping("/delete/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        if (userService.findByEmailForCheck(email).isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        userService.deleteUser(email);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/add-buck/{email}/{bucks}")
    @Transactional
    public ResponseEntity<?> addBucksToUser(@PathVariable String email, @PathVariable Long bucks) {
        userService.addBucksToUser(email, bucks);
        return ResponseEntity.ok("bucks added");
    }
}
