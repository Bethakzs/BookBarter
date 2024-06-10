package com.example.authservice.service;

import com.example.authservice.dto.request.JwtRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    public void whenUserTriesToLoginWithWrongPassword_thenAuthenticationFails() {
        // Try to log in with the wrong password
        JwtRequest authRequest = new JwtRequest("test@test.com", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        ResponseEntity<?> response = authService.createAuthToken(authRequest);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void whenInvalidRefreshToken_thenRefreshAuthTokenFails() {
        // Спробуємо оновити токен з недійсним токеном оновлення
        ResponseEntity<?> response = authService.refreshAuthToken("invalidRefreshToken");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

//    @Test
//    public void whenValidRefreshToken_thenLogoutSucceeds() {
//        // Створюємо нового користувача
//        UserRegistration regRequest = new UserRegistration("testuser", "test@test.com", "password", "1234567890");
//        when(userService.findByEmailForCheck(regRequest.getEmail())).thenReturn(Optional.empty());
//        User user = new User();
//        when(userService.createUser(regRequest)).thenReturn(user);
//
//        // Перевіряємо, що користувач може успішно зареєструватися
//        ResponseEntity<?> regResponse = authService.createNewUser(regRequest);
//        assertEquals(HttpStatus.OK, regResponse.getStatusCode());
//
//        // Тепер перевіряємо, що користувач може вийти з системи
//        when(userService.findByRefreshToken(user.getRefreshToken())).thenReturn(user);
//        ResponseEntity<?> logoutResponse = authService.logoutUser(user.getRefreshToken());
//        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());
//    }

    @Test
    public void whenInvalidRefreshToken_thenLogoutFails() {
        // Спробуємо вийти з системи з недійсним токеном оновлення
        ResponseEntity<?> response = authService.logoutUser("invalidRefreshToken");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
