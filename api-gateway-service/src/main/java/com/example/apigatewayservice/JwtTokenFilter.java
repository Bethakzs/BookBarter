package com.example.apigatewayservice;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenFilter implements WebFilter {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .map(authHeader -> authHeader.substring(7))
                .flatMap(jwt -> {
                    try {
                        String email = jwtTokenService.getEmail(jwt);
                        List<Role> roles = jwtTokenService.getRoles(jwt); // змінено на список ролей
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
                })
                .switchIfEmpty(chain.filter(exchange));
    }
}