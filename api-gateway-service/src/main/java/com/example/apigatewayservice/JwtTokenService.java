package com.example.apigatewayservice;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String secret;

    public String getEmail(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public List<Role> getRoles(String token) { // змінено на список ролей
        List<String> roleStrings = getAllClaimsFromToken(token).get("roles", List.class);
        return roleStrings.stream().map(Role::valueOf).collect(Collectors.toList());
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}