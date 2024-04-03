package com.example.authservice.controller;

import com.example.authservice.dto.JwtRequest;
import com.example.authservice.dto.JwtResponse;
import com.example.authservice.service.AuthService;
import com.example.authservice.entity.Role;
import com.example.authservice.dto.UserRegistration;
import com.example.authservice.service.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> createNewUser(@RequestBody UserRegistration regRequest, HttpServletResponse response) {
        return createResponseEntity(authService.createNewUser(regRequest), response);
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
            List<Integer> roleValues = jwtResponse.getRoles().stream().map(Role::getValue).collect(Collectors.toList());
            responseBody.put("roles", roleValues);
            responseBody.put("accessToken", jwtResponse.getJwtAccessToken());
            return ResponseEntity.ok(responseBody);
        } else {
            return responseEntity;
        }
    }
}
