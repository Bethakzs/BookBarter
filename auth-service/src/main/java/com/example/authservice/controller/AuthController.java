package com.example.authservice.controller;

import com.example.authservice.dto.JwtRequest;
import com.example.authservice.dto.JwtResponse;
import com.example.authservice.entity.User;
import com.example.authservice.exception.AppError;
import com.example.authservice.service.AuthService;
import com.example.authservice.entity.Role;
import com.example.authservice.dto.UserRegistration;
import com.example.authservice.service.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @Transactional
    public ResponseEntity<?> createNewUser(@RequestParam("login") String login,
                                           @RequestParam("email") String email,
                                           @RequestParam("pwd") String pwd,
                                           @RequestParam("phone") String phone,
                                           @RequestParam(value = ("image"), required = false) MultipartFile image,
                                           HttpServletResponse response) {
        UserRegistration regRequest = new UserRegistration(login, email, pwd, phone, image);
        return createResponseEntity(authService.createNewUser(regRequest), response);
    }

    @PostMapping("/get/{number}")
    @Transactional
    public ResponseEntity<?> findByNumber(@PathVariable String number) {
        Optional<User> user = authService.findByNumber(number);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refreshAuthToken(@CookieValue("jwt") String refreshToken, HttpServletResponse response) {
        System.out.println("refreshToken: " + refreshToken);
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
            List<Integer> roleValue = jwtTokenService.getRoles(jwtResponse.getJwtAccessToken()).stream()
                        .map(Role::getValue)
                        .collect(Collectors.toList());
            responseBody.put("roles", roleValue);
            responseBody.put("accessToken", jwtResponse.getJwtAccessToken());
            return ResponseEntity.ok(responseBody);
        } else {
            if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
                return ResponseEntity.noContent().build();
            } else if (responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password or email");
            } else if (responseEntity.getStatusCode() == HttpStatus.CONFLICT) {
                AppError appError = (AppError) responseEntity.getBody();
                assert appError != null;
                return ResponseEntity.status(HttpStatus.CONFLICT).body(appError.getMessage());
            } else if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User with the credentials already exists");
            } else if (responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
            } else {
                return ResponseEntity.ok(responseEntity.getBody());
            }
        }
    }
}
