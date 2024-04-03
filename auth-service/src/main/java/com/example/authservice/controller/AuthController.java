package com.example.authservice.controller;

import com.example.authservice.dto.JwtRequest;
import com.example.authservice.dto.JwtResponse;
import com.example.authservice.service.AuthService;
import com.example.authservice.entity.Role;
import com.example.authservice.dto.UserRegistration;
import com.example.authservice.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthToken(@RequestBody JwtRequest authRequest, HttpServletResponse response) {
        return createResponseEntity(authService.createAuthToken(authRequest), response);
    }

    @PostMapping("/registration")
    public ResponseEntity<?> registerUser(
            @RequestPart("login") String login,
            @RequestPart("pwd") String pwd,
            @RequestPart("email") String email,
            @RequestPart("phone") String phone,
            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletResponse response) {
        UserRegistration userRegistration = new UserRegistration(login, email, pwd, phone, image);;
        return ResponseEntity.ok(createResponseEntity(authService.createNewUser(userRegistration), response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAuthToken(@CookieValue("jwt") String refreshToken, HttpServletResponse response) {
        return createResponseEntity(authService.refreshAuthToken(refreshToken), response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> handleLogout(@CookieValue("jwt") String refreshToken, HttpServletResponse response) {
        ResponseEntity<?> responseEntity = authService.logoutUser(refreshToken);
        if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
            response.setHeader("Set-Cookie", "jwt=; HttpOnly; SameSite=None; Secure; Max-age=0");
            return ResponseEntity.noContent().build();
        } else {
            return responseEntity;
        }
    }

    private ResponseEntity<?> createResponseEntity(ResponseEntity<?> responseEntity, HttpServletResponse response) {
        if (responseEntity.getBody() instanceof JwtResponse jwtResponse) {
            jwtTokenService.setTokenCookies(response, jwtResponse);
            Map<String, Object> responseBody = new HashMap<>();
            Integer roleValue = jwtResponse.getRoles().ordinal();
            responseBody.put("roles", roleValue);
            responseBody.put("accessToken", jwtResponse.getJwtAccessToken());
            return ResponseEntity.ok(responseBody);
        } else {
            return ResponseEntity.ok(responseEntity.getBody());
        }
    }
}
