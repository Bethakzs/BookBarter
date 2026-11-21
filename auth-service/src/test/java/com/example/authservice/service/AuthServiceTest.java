package com.example.authservice.service;

import com.example.authservice.dto.request.JwtRequest;
import com.example.authservice.dto.request.UserRegistration;
import com.example.authservice.dto.response.JwtResponse;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.exception.AppError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "test@test.com";

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    private static User user(Set<Role> roles) {
        return User.builder()
                .id(1L)
                .login("tester")
                .email(EMAIL)
                .pwd("encoded")
                .phone("+380000000000")
                .rating(3.5)
                .bucks(5L)
                .roles(roles)
                .build();
    }

    private static UserDetails userDetails() {
        return new org.springframework.security.core.userdetails.User(
                EMAIL, "encoded", List.of(new SimpleGrantedAuthority(Role.ROLE_USER.name())));
    }

    // ---------- createAuthToken ----------

    @Test
    void createAuthToken_wrongPassword_returnsUnauthorized() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<?> response = authService.createAuthToken(new JwtRequest(EMAIL, "wrongpassword"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(AppError.class);
        assertThat(((AppError) response.getBody()).getMessage()).isEqualTo("Incorrect email or password");
        verifyNoInteractions(userService, jwtTokenService);
    }

    @Test
    void createAuthToken_validCredentials_returnsTokensAndStoresRefreshToken() {
        User user = user(new HashSet<>(Set.of(Role.ROLE_USER)));
        UserDetails details = userDetails();
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.of(user));
        when(userService.loadUserByUsername(EMAIL)).thenReturn(details);
        when(jwtTokenService.generateToken(details)).thenReturn("access");
        when(jwtTokenService.generateRefreshToken(details)).thenReturn("refresh");

        ResponseEntity<?> response = authService.createAuthToken(new JwtRequest(EMAIL, "secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JwtResponse body = (JwtResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getJwtAccessToken()).isEqualTo("access");
        assertThat(body.getJwtRefreshToken()).isEqualTo("refresh");
        assertThat(body.getRoles()).containsExactly(Role.ROLE_USER);
        assertThat(user.getRefreshToken()).isEqualTo("refresh");
        verify(userService, times(1)).updateUser(user);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo(EMAIL);
        assertThat(captor.getValue().getCredentials()).isEqualTo("secret");
    }

    @Test
    void createAuthToken_userMissingInAuthDb_returnsNotFound() {
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.createAuthToken(new JwtRequest(EMAIL, "secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(jwtTokenService);
        verify(userService, never()).updateUser(any());
    }

    @Test
    void createAuthToken_userWithoutRoles_getsDefaultRoleUser() {
        User user = user(null);
        UserDetails details = userDetails();
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.of(user));
        when(userService.loadUserByUsername(EMAIL)).thenReturn(details);
        when(jwtTokenService.generateToken(details)).thenReturn("access");
        when(jwtTokenService.generateRefreshToken(details)).thenReturn("refresh");

        ResponseEntity<?> response = authService.createAuthToken(new JwtRequest(EMAIL, "secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((JwtResponse) response.getBody()).getRoles()).containsExactly(Role.ROLE_USER);
        assertThat(user.getRoles()).containsExactly(Role.ROLE_USER);
        // once for refresh token, once for default roles
        verify(userService, times(2)).updateUser(user);
    }

    @Test
    void createAuthToken_failingRefreshTokenUpdate_isSwallowed() {
        User user = user(new HashSet<>(Set.of(Role.ROLE_ADMIN)));
        UserDetails details = userDetails();
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.of(user));
        when(userService.loadUserByUsername(EMAIL)).thenReturn(details);
        when(jwtTokenService.generateToken(details)).thenReturn("access");
        when(jwtTokenService.generateRefreshToken(details)).thenReturn("refresh");
        doThrow(new RuntimeException("db down")).when(userService).updateUser(user);

        ResponseEntity<?> response = authService.createAuthToken(new JwtRequest(EMAIL, "secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((JwtResponse) response.getBody()).getRoles()).containsExactly(Role.ROLE_ADMIN);
    }

    @Test
    void createAuthToken_unexpectedError_returnsInternalServerError() {
        User user = user(new HashSet<>(Set.of(Role.ROLE_USER)));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.of(user));
        when(userService.loadUserByUsername(EMAIL)).thenThrow(new RuntimeException("kafka timeout"));

        ResponseEntity<?> response = authService.createAuthToken(new JwtRequest(EMAIL, "secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(((AppError) response.getBody()).getMessage()).contains("kafka timeout");
    }

    // ---------- createNewUser ----------

    private static UserRegistration registration() {
        return UserRegistration.builder()
                .login("tester")
                .email(EMAIL)
                .pwd("secret")
                .phone("+380000000000")
                .build();
    }

    @Test
    void createNewUser_existingEmail_returnsUnauthorized() {
        UserRegistration reg = registration();
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.of(user(Set.of(Role.ROLE_USER))));
        when(userService.findByLoginForCheck("tester")).thenReturn(Optional.empty());
        when(userService.findByPhoneForCheck("+380000000000")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.createNewUser(reg);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(userService, never()).createUser(any());
    }

    @Test
    void createNewUser_existingPhone_returnsUnauthorized() {
        UserRegistration reg = registration();
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.empty());
        when(userService.findByLoginForCheck("tester")).thenReturn(Optional.empty());
        when(userService.findByPhoneForCheck("+380000000000")).thenReturn(Optional.of(user(Set.of(Role.ROLE_USER))));

        ResponseEntity<?> response = authService.createNewUser(reg);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(userService, never()).createUser(any());
    }

    @Test
    void createNewUser_newUser_isCreatedAndRolesReturned() {
        UserRegistration reg = registration();
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(Optional.empty());
        when(userService.findByLoginForCheck("tester")).thenReturn(Optional.empty());
        when(userService.findByPhoneForCheck("+380000000000")).thenReturn(Optional.empty());
        when(userService.createUser(reg)).thenReturn(user(Set.of(Role.ROLE_USER)));

        ResponseEntity<?> response = authService.createNewUser(reg);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Set.of(Role.ROLE_USER));
        verify(userService).createUser(reg);
    }

    // ---------- refreshAuthToken ----------

    @Test
    void refreshAuthToken_unknownToken_returnsBadRequest() {
        when(userService.findByRefreshToken("invalidRefreshToken")).thenReturn(null);

        ResponseEntity<?> response = authService.refreshAuthToken("invalidRefreshToken");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void refreshAuthToken_tokenFailsValidation_returnsUnauthorized() {
        User user = user(Set.of(Role.ROLE_USER));
        when(userService.findByRefreshToken("stale")).thenReturn(user);
        when(jwtTokenService.validateToken("stale", EMAIL)).thenReturn(false);

        ResponseEntity<?> response = authService.refreshAuthToken("stale");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(userService, never()).updateUser(any());
    }

    @Test
    void refreshAuthToken_validToken_rotatesTokens() {
        User user = user(Set.of(Role.ROLE_USER));
        user.setRefreshToken("old-refresh");
        UserDetails details = userDetails();
        when(userService.findByRefreshToken("old-refresh")).thenReturn(user);
        when(jwtTokenService.validateToken("old-refresh", EMAIL)).thenReturn(true);
        when(userService.loadUserByUsername(EMAIL)).thenReturn(details);
        when(jwtTokenService.generateToken(details)).thenReturn("new-access");
        when(jwtTokenService.generateRefreshToken(details)).thenReturn("new-refresh");

        ResponseEntity<?> response = authService.refreshAuthToken("old-refresh");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JwtResponse body = (JwtResponse) response.getBody();
        assertThat(body.getJwtAccessToken()).isEqualTo("new-access");
        assertThat(body.getJwtRefreshToken()).isEqualTo("new-refresh");
        assertThat(user.getRefreshToken()).isEqualTo("new-refresh");
        verify(userService).updateUser(user);
    }

    // ---------- logoutUser ----------

    @Test
    void logoutUser_unknownToken_returnsBadRequest() {
        when(userService.findByRefreshToken("invalidRefreshToken")).thenReturn(null);

        ResponseEntity<?> response = authService.logoutUser("invalidRefreshToken");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userService, never()).updateUser(any());
    }

    @Test
    void logoutUser_validToken_clearsRefreshToken() {
        User user = user(Set.of(Role.ROLE_USER));
        user.setRefreshToken("refresh");
        when(userService.findByRefreshToken("refresh")).thenReturn(user);

        ResponseEntity<?> response = authService.logoutUser("refresh");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(user.getRefreshToken()).isEmpty();
        verify(userService).updateUser(user);
    }

    // ---------- findByNumber ----------

    @Test
    void findByNumber_delegatesToUserService() {
        User user = user(Set.of(Role.ROLE_USER));
        when(userService.findByPhoneForCheck("+380000000000")).thenReturn(Optional.of(user));

        assertThat(authService.findByNumber("+380000000000")).contains(user);
    }
}
