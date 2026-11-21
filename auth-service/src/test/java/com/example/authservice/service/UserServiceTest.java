package com.example.authservice.service;

import com.example.authservice.dao.UserRepository;
import com.example.authservice.dto.request.UserRegistration;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.kafka.ReplyProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "test@test.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReplyProcessor replyProcessor;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private UserService userService;

    private void replyWith(String json) {
        when(replyProcessor.waitForReply()).thenReturn(CompletableFuture.completedFuture(json));
    }

    private static Set<String> authorities(UserDetails details) {
        Set<String> result = new HashSet<>();
        for (GrantedAuthority authority : details.getAuthorities()) {
            result.add(authority.getAuthority());
        }
        return result;
    }

    @Test
    void createUser_encodesPasswordSetsDefaultsAndPublishesRegistrationEmail() {
        UserRegistration reg = UserRegistration.builder()
                .login("tester").email(EMAIL).pwd("plain").phone("+380000000000").build();
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.createUser(reg);

        assertThat(saved.getLogin()).isEqualTo("tester");
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getPwd()).isEqualTo("encoded");
        assertThat(saved.getBucks()).isEqualTo(5L);
        assertThat(saved.getRating()).isEqualTo(3.5);
        assertThat(saved.getRoles()).containsExactly(Role.ROLE_USER);
        assertThat(saved.getImage()).isNull();
        assertThat(saved.isNotifications()).isFalse();

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(message.capture());
        assertThat(message.getValue().getPayload()).isEqualTo(EMAIL);
        assertThat(message.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("email-service-request-register");
    }

    @Test
    void createUser_withImage_storesImageBytes() {
        byte[] bytes = {1, 2, 3};
        UserRegistration reg = UserRegistration.builder()
                .login("tester").email(EMAIL).pwd("plain").phone("1")
                .image(new MockMultipartFile("image", bytes)).build();
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.createUser(reg);

        assertThat(saved.getImage()).containsExactly(1, 2, 3);
    }

    @Test
    void loadUserByUsername_userWithRoles_mapsRolesToAuthorities() {
        replyWith("{\"login\":\"tester\",\"email\":\"" + EMAIL + "\",\"pwd\":\"encoded\",\"roles\":[\"ROLE_ADMIN\"]}");

        UserDetails details = userService.loadUserByUsername(EMAIL);

        assertThat(details.getUsername()).isEqualTo(EMAIL);
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(authorities(details)).containsExactly("ROLE_ADMIN");
        verify(userRepository, never()).findByLogin(any());
    }

    @Test
    void loadUserByUsername_userWithoutRolesAndNotInDb_defaultsToRoleUser() {
        replyWith("{\"login\":\"tester\",\"email\":\"" + EMAIL + "\",\"pwd\":\"encoded\"}");
        when(userRepository.findByLogin("tester")).thenReturn(Optional.empty());

        UserDetails details = userService.loadUserByUsername(EMAIL);

        assertThat(authorities(details)).containsExactly("ROLE_USER");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserByUsername_userWithoutRolesButInDb_repairsDbRoles() {
        replyWith("{\"login\":\"tester\",\"email\":\"" + EMAIL + "\",\"pwd\":\"encoded\"}");
        User dbUser = User.builder().login("tester").email(EMAIL).pwd("db-encoded").roles(null).build();
        when(userRepository.findByLogin("tester")).thenReturn(Optional.of(dbUser));

        UserDetails details = userService.loadUserByUsername(EMAIL);

        assertThat(dbUser.getRoles()).containsExactly(Role.ROLE_USER);
        verify(userRepository).save(dbUser);
        assertThat(details.getPassword()).isEqualTo("db-encoded");
        assertThat(authorities(details)).containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_nullReply_throwsUsernameNotFound() {
        replyWith("null");

        assertThatThrownBy(() -> userService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_malformedReply_throwsRuntimeException() {
        replyWith("{not json");

        assertThatThrownBy(() -> userService.loadUserByUsername(EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error deserializing user");
    }

    @Test
    void finders_delegateToRepository() {
        User user = User.builder().id(7L).email(EMAIL).login("tester").phone("1").build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByLogin("tester")).thenReturn(Optional.of(user));
        when(userRepository.findByPhone("1")).thenReturn(Optional.empty());
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.findByRefreshToken("rt")).thenReturn(user);

        assertThat(userService.findByEmailForCheck(EMAIL)).contains(user);
        assertThat(userService.findByLoginForCheck("tester")).contains(user);
        assertThat(userService.findByPhoneForCheck("1")).isEmpty();
        assertThat(userService.findByIdForCheck(7L)).contains(user);
        assertThat(userService.findByRefreshToken("rt")).isSameAs(user);
    }

    @Test
    void updateUser_savesUser() {
        User user = User.builder().email(EMAIL).build();

        userService.updateUser(user);

        verify(userRepository).save(user);
    }
}
