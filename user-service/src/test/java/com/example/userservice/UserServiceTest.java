package com.example.userservice;

import com.example.userservice.dao.UserDAO;
import com.example.userservice.dto.request.BookDTO;
import com.example.userservice.dto.response.UserDTO;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.kafka.ReplyProcessor;
import com.example.userservice.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "reader@bookbarter.com";

    @Mock
    private UserDAO userRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ReplyProcessor replyProcessor;

    @InjectMocks
    private UserServiceImpl userService;

    private static User user() {
        return User.builder()
                .id(1L)
                .login("reader")
                .email(EMAIL)
                .pwd("encoded")
                .phone("+380000000000")
                .rating(4.5)
                .bucks(10L)
                .roles(Set.of(Role.ROLE_USER))
                .notifications(true)
                .build();
    }

    private static CompletableFuture<String> reply(String json) {
        return CompletableFuture.completedFuture(json);
    }

    private static String bookJson(long id, String status) {
        return "{\"id\":" + id + ",\"title\":\"Book " + id + "\",\"status\":\"" + status
                + "\",\"price\":3,\"year\":2001,\"userEmail\":\"" + EMAIL + "\"}";
    }

    @Test
    void findByEmail_existingUser_returnsUser() throws Exception {
        User user = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThat(userService.findByEmail(EMAIL)).isSameAs(user);
    }

    @Test
    void findByEmail_missingUser_throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail(EMAIL)).hasMessage("User not found");
    }

    @Test
    void findByEmailForCheck_missingUser_throwsIllegalArgument() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmailForCheck(EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void findByEmailWithOutCheck_missingUser_returnsNull() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(userService.findByEmailWithOutCheck(EMAIL)).isNull();
    }

    @Test
    void findByEmailUserDTO_returnsProfileWithOnlyAvailableBooks() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(replyProcessor.waitForReply()).thenReturn(
                reply("[1, 2, 3]"),
                reply(bookJson(1, "AVAILABLE")),
                reply(bookJson(2, "RESERVED")),
                reply(bookJson(3, "SOLD")));

        UserDTO dto = userService.findByEmailUserDTO(EMAIL);

        assertThat(dto.getLogin()).isEqualTo("reader");
        assertThat(dto.getEmail()).isEqualTo(EMAIL);
        assertThat(dto.getPhone()).isEqualTo("+380000000000");
        assertThat(dto.getRating()).isEqualTo(4.5);
        assertThat(dto.getBucks()).isEqualTo(10L);
        assertThat(dto.isNotifications()).isTrue();
        assertThat(dto.getBooks()).extracting(BookDTO::getId).containsExactly(1L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(4)).send(messages.capture());
        List<Message> sent = messages.getAllValues();
        assertThat(sent.get(0).getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("book-service-request-get-all-books-id");
        assertThat(sent.get(0).getPayload()).isEqualTo(EMAIL);
        assertThat(sent.get(1).getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("book-service-request-get-book-by-id-topic");
        assertThat(sent.get(1).getPayload()).isEqualTo("1");
    }

    @Test
    void findByEmailUserDTO_userWithoutBooks_returnsEmptyBookList() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(replyProcessor.waitForReply()).thenReturn(reply("[]"));

        UserDTO dto = userService.findByEmailUserDTO(EMAIL);

        assertThat(dto.getBooks()).isEmpty();
        verify(kafkaTemplate, times(1)).send(any(Message.class));
    }

    @Test
    void findByEmailUserDTO_missingUser_throwsWithoutCallingKafka() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmailUserDTO(EMAIL)).hasMessage("User not found");
        verifyNoInteractions(kafkaTemplate, replyProcessor);
    }

    @Test
    void findByEmailUserDTO_malformedBookReply_throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(replyProcessor.waitForReply()).thenReturn(reply("[5]"), reply("not-json"));

        assertThatThrownBy(() -> userService.findByEmailUserDTO(EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error deserializing book");
    }

    @Test
    void addBucksToUser_increasesBalanceAndSaves() {
        User user = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        userService.addBucksToUser(EMAIL, 5L);

        assertThat(user.getBucks()).isEqualTo(15L);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_andDeleteUser_delegateToRepository() {
        User user = user();

        userService.updateUser(user);
        userService.deleteUser(EMAIL);

        verify(userRepository).save(user);
        verify(userRepository).deleteByEmail(EMAIL);
        verify(userRepository, never()).delete(any());
    }
}
