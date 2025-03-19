package com.example.authservice.service;

import com.example.authservice.dto.request.JwtRequest;
import com.example.authservice.dto.request.UserRegistration;
import com.example.authservice.dto.response.JwtResponse;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.exception.AppError;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @Transactional
    public ResponseEntity<?> createAuthToken(JwtRequest authRequest) {
        System.out.println("Processing auth request: " + authRequest);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPwd()));
            System.out.println("Authentication successful for: " + authRequest.getEmail());
            
            Optional<User> userOpt = userService.findByEmailForCheck(authRequest.getEmail());
            if (!userOpt.isPresent()) {
                System.out.println("User not found for email: " + authRequest.getEmail());
                return new ResponseEntity<>(new AppError(HttpStatus.NOT_FOUND.value(), "User not found"), HttpStatus.NOT_FOUND);
            }
            
            User user = userOpt.get();
            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
            String accessToken = jwtTokenService.generateToken(userDetails);
            String refreshToken = jwtTokenService.generateRefreshToken(userDetails);
            System.out.println("Tokens generated for user: " + user.getEmail());
            
            user.setRefreshToken(refreshToken);
            try {
                userService.updateUser(user);
            } catch (Exception e) {
                System.out.println("Error updating user refresh token: " + e.getMessage());
            }
            
            Set<Role> roles = user.getRoles();
            if (roles == null || roles.isEmpty()) {
                System.out.println("User has no roles, adding default ROLE_USER");
                roles = new HashSet<>(Collections.singleton(Role.ROLE_USER));
                user.setRoles(roles);
                try {
                    userService.updateUser(user);
                } catch (Exception e) {
                    System.out.println("Error updating user roles: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok().body(new JwtResponse(accessToken, refreshToken, roles));
        } catch (BadCredentialsException e) {
            System.out.println("Bad credentials for: " + authRequest.getEmail());
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Incorrect email or password"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            System.out.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Authentication error: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ResponseEntity<?> createNewUser(UserRegistration regRequest) {
        Optional<User> existingUserEmail = userService.findByEmailForCheck(regRequest.getEmail());
        Optional<User> existingUserLogin = userService.findByLoginForCheck(regRequest.getLogin());
        Optional<User> existingUserPhone = userService.findByPhoneForCheck(regRequest.getPhone());
        if (existingUserEmail.isPresent() || existingUserLogin.isPresent() || existingUserPhone.isPresent()) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "User with this credentials already exists"), HttpStatus.UNAUTHORIZED);
        }
        User user = userService.createUser(regRequest);
        Set<Role> roles = user.getRoles();
        return ResponseEntity.ok().body(roles);
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
        System.out.println("refreshToken updated");
        return ResponseEntity.ok().body(new JwtResponse(newAccessToken, newRefreshToken, roles));
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

    public Optional<User> findByNumber(String number) {
        return userService.findByPhoneForCheck(number);
    }
}


