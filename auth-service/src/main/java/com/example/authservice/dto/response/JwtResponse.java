package com.example.authservice.dto.response;

import com.example.authservice.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class JwtResponse {
    private String jwtAccessToken;
    private String jwtRefreshToken;
    private Set<Role> roles;
}
