package com.example.authservice.service;

import com.example.authservice.entity.Role;
import com.example.authservice.dto.JwtResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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

    @Value("${jwt.accessToken.lifetime}")
    private Duration jwtAccessTokenLifetime;

    @Value("${jwt.refreshToken.lifetime}")
    private Duration jwtRefreshTokenLifetime;

    public void setTokenCookies(HttpServletResponse response, JwtResponse jwtResponse) {
        Cookie refreshTokenCookie = new Cookie("jwt", jwtResponse.getJwtRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge((int) jwtRefreshTokenLifetime.toHours());
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    }

//    public void setTokenCookies(HttpServletResponse response, JwtResponse jwtResponse) {
//        Cookie refreshTokenCookie = new Cookie("jwt", jwtResponse.getJwtRefreshToken());
//        refreshTokenCookie.setHttpOnly(true);
//        refreshTokenCookie.setMaxAge((int) jwtRefreshTokenLifetime.toHours());
//        refreshTokenCookie.setSecure(false); // Встановіть це значення як true
//        refreshTokenCookie.setPath("/");
//        // Додайте цей рядок для встановлення SameSite
////        response.setHeader("Set-Cookie", "SameSite=Strict; Secure");
//        response.addCookie(refreshTokenCookie);
//    }

    public String generateToken(UserDetails userDetails) {
        return generateJwt(userDetails, jwtAccessTokenLifetime);
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

    public boolean validateToken(String token, String email) {
        final String extractedEmail = getEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateJwt(userDetails, jwtRefreshTokenLifetime);
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getAllClaimsFromToken(token).getExpiration();
        return expiration.before(new Date());
    }

    private String generateJwt(UserDetails userDetails, Duration lifetime) {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        List<Role> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Role::valueOf)
                .collect(Collectors.toList());
        claims.put("roles", roles);

        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + lifetime.toMillis());
        return Jwts.builder()
                .setHeader(header)
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(issuedDate)
                .setExpiration(expiredDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
}
