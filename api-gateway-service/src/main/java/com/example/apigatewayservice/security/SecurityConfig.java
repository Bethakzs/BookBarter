package com.example.apigatewayservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges ->
                        exchanges
                                .pathMatchers("/auth/**", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui.html",
                                        "/webjars/**", "/swagger-ui/**", "/swagger-ui.html**", "/swagger-ui/**", "/v3/api-docs",
                                        "/v3/api-docs/**", "/swagger-ui.html**", "/swagger-ui/**").permitAll()
                                .pathMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                )
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((swe, ex) -> Mono.fromRunnable(() -> {
                            ServerHttpResponse response = swe.getResponse();
                            HttpHeaders headers = response.getHeaders();
                            if (!headers.containsKey("WWW-Authenticate")) {
                                response = new ServerHttpResponseDecorator(response) {
                                    @Override
                                    public HttpHeaders getHeaders() {
                                        HttpHeaders httpHeaders = new HttpHeaders();
                                        httpHeaders.putAll(super.getHeaders());
                                        httpHeaders.add("WWW-Authenticate", "Bearer");
                                        return httpHeaders;
                                    }
                                };
                            }
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        }))
                )
                .addFilterAt(corsWebFilter(), SecurityWebFiltersOrder.CORS)
                .addFilterAt(jwtTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:4173", "http://localhost:5173"));
        corsConfig.setAllowedMethods(List.of("*"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}