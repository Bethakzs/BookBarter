package com.example.apigatewayservice.security;

import com.example.apigatewayservice.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    private final String secret;

    @Autowired
    public JwtTokenService(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    public String getEmail(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public List<Role> getRoles(String token) {
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
