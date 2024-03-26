package com.example.authservice;

import com.example.authservice.dto.JwtRequest;
import com.example.authservice.dto.JwtResponse;
import com.example.authservice.dto.UserRegistration;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/login")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/auth")
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
            int roleValue = jwtResponse.getRole().getValue();
            responseBody.put("role", roleValue);
            responseBody.put("accessToken", jwtResponse.getJwtAccessToken());
            return ResponseEntity.ok(responseBody);
        } else {
            return responseEntity;
        }
    }

}
