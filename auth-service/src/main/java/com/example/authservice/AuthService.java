package com.example.authservice;

import com.example.authservice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    public ResponseEntity<?> createAuthToken(@RequestBody JwtRequest authRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPwd()));
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password"), HttpStatus.UNAUTHORIZED);
        }
        User user = userService.findByEmailForCheck(authRequest.getEmail()).get();
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPwd(), new ArrayList<>());
        String accessToken = jwtTokenService.generateToken(userDetails);
        String refreshToken = jwtTokenService.generateRefreshToken(userDetails);
        user.setRefreshToken(refreshToken);
        userService.updateUser(user);
        Role role = user.getRole();
        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken, role));
    }

    public ResponseEntity<?> createNewUser(@RequestBody UserRegistration regRequest) {
        if (userService.findByEmailForCheck(regRequest.getEmail()).isPresent()) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "User with the specified email already exists"), HttpStatus.BAD_REQUEST);
        }
        User user = userService.createUser(regRequest);
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenService.generateToken(userDetails);
        String refreshToken = jwtTokenService.generateRefreshToken(userDetails);
        User userWithToken = userService.findByEmailForCheck(user.getEmail()).get();
        userWithToken.setRefreshToken(refreshToken);
        userService.updateUser(userWithToken);
        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken, userWithToken.getRole()));
    }

    public ResponseEntity<?> refreshAuthToken(String refreshToken) {
        User user = userService.findByRefreshToken(refreshToken);
        if (user == null) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "Invalid refreshToken"), HttpStatus.BAD_REQUEST);
        }
        if (!jwtTokenService.validateToken(refreshToken, user.getEmail())) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Invalid refreshToken"), HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtTokenService.generateToken(userDetails);
        String newRefreshToken = jwtTokenService.generateRefreshToken(userDetails);
        user.setRefreshToken(newRefreshToken);
        userService.updateUser(user);
        Role role = user.getRole();
        return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken, role));
    }

    public ResponseEntity<?> logoutUser(String refreshToken) {
        User user = userService.findByRefreshToken(refreshToken);
        if (user == null) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "Invalid refreshToken"), HttpStatus.BAD_REQUEST);
        }
        user.setRefreshToken("");
        userService.updateUser(user);
        return ResponseEntity.noContent().build();
    }
}


