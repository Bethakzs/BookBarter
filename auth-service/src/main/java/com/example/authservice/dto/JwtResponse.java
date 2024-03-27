package com.example.authservice.dto;

import com.example.authservice.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class JwtResponse {
    private String jwtAccessToken;
    private String jwtRefreshToken;
    private Role role;
}
