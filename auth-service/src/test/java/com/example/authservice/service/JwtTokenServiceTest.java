package com.example.authservice.service;

import com.example.authservice.dto.response.JwtResponse;
import com.example.authservice.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String EMAIL = "test@test.com";

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService();
        ReflectionTestUtils.setField(jwtTokenService, "secret", "dW5pdC10ZXN0LXNlY3JldC1rZXktZm9yLWJvb2tiYXJ0ZXI=");
        ReflectionTestUtils.setField(jwtTokenService, "jwtAccessTokenLifetime", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(jwtTokenService, "jwtRefreshTokenLifetime", Duration.ofDays(1));
    }

    private static UserDetails userDetails(String... roles) {
        return new User(EMAIL, "pwd", java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void generateToken_containsSubjectAndRoles() {
        String token = jwtTokenService.generateToken(userDetails("ROLE_USER", "ROLE_ADMIN"));

        assertThat(jwtTokenService.getEmail(token)).isEqualTo(EMAIL);
        assertThat(jwtTokenService.getRoles(token)).containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_ADMIN);
    }

    @Test
    void validateToken_matchingEmail_isValid() {
        String token = jwtTokenService.generateRefreshToken(userDetails("ROLE_USER"));

        assertThat(jwtTokenService.validateToken(token, EMAIL)).isTrue();
    }

    @Test
    void validateToken_otherEmail_isInvalid() {
        String token = jwtTokenService.generateToken(userDetails("ROLE_USER"));

        assertThat(jwtTokenService.validateToken(token, "someone@else.com")).isFalse();
    }

    @Test
    void setTokenCookies_setsHttpOnlyRefreshCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtTokenService.setTokenCookies(response, new JwtResponse("access", "refresh", Set.of(Role.ROLE_USER)));

        String cookie = response.getHeader("Set-Cookie");
        assertThat(cookie)
                .startsWith("jwt=refresh;")
                .contains("HttpOnly", "Secure", "SameSite=None", "Max-Age=86400");
    }

    @Test
    void generateToken_rolesClaimRoundTrips() {
        String token = jwtTokenService.generateToken(userDetails("ROLE_EDITOR"));

        List<Role> roles = jwtTokenService.getRoles(token);

        assertThat(roles).containsExactly(Role.ROLE_EDITOR);
    }
}
