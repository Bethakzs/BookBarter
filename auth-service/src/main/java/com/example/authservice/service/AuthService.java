package com.example.authservice.service;

import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.dto.*;
import com.example.authservice.exception.AppError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public ResponseEntity<?> createAuthToken(JwtRequest authRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPwd()));
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password"), HttpStatus.UNAUTHORIZED);
        }
        User user = userService.findByEmailForCheck(authRequest.getEmail()).get();
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenService.generateToken(userDetails);
        String refreshToken = jwtTokenService.generateRefreshToken(userDetails);
        user.setRefreshToken(refreshToken);
        userService.updateUser(user);
        Set<Role> roles = user.getRoles();
        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken, roles));
    }

    public ResponseEntity<?> createNewUser(UserRegistration regRequest) {
        if (userService.findByEmailForCheck(regRequest.getEmail()).isPresent()) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), "User with the specified email already exists"), HttpStatus.BAD_REQUEST);
        }
        User user = userService.createUser(regRequest);
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenService.generateToken(userDetails);
        String refreshToken = jwtTokenService.generateRefreshToken(userDetails);
        Optional<User> optionalUser = userService.findByEmailForCheck(user.getEmail());
        if (optionalUser.isPresent()) {
            User userWithToken = optionalUser.get();
            userWithToken.setRefreshToken(refreshToken);
            userService.updateUser(userWithToken);
        } else {
            user.setRefreshToken(refreshToken);
            userService.updateUser(user);
        }
        Set<Role> roles = user.getRoles();
        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken, roles));
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
        Set<Role> roles = user.getRoles();
        return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken, roles));
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


