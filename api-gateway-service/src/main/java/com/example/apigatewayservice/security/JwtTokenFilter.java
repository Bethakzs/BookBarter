package com.example.apigatewayservice.security;

import com.example.apigatewayservice.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenFilter implements WebFilter {

    private final JwtTokenService jwtTokenService;

    @Autowired
    public JwtTokenFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .map(authHeader -> authHeader.substring(7))
                .flatMap(jwt -> processJwtToken(jwt, exchange, chain))
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> processJwtToken(String jwt, ServerWebExchange exchange, WebFilterChain chain) {
        try {
            String email = jwtTokenService.getEmail(jwt);
            List<Role> roles = jwtTokenService.getRoles(jwt);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.name()))
                    .collect(Collectors.toList());
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    authorities
            );
            SecurityContextImpl securityContext = new SecurityContextImpl(token);
            return chain.filter(exchange).contextWrite(context -> context.put(SecurityContext.class, Mono.just(securityContext)));
        } catch (ExpiredJwtException e) {
            System.err.println("Token expired");
            return chain.filter(exchange);
        }
    }
}
