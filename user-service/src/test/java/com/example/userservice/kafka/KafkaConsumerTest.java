package com.example.userservice.kafka;

import com.example.userservice.dao.UserDAO;
import com.example.userservice.entity.User;
import com.example.userservice.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    private static final String EMAIL = "reader@bookbarter.com";

    @Mock
    private UserServiceImpl userService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    private static User user(double rating, long bucks) {
        return User.builder().login("reader").email(EMAIL).pwd("encoded").phone("1").rating(rating).bucks(bucks).build();
    }

    @Test
    void handleGetUserFromReviewService_repliesWithSerializedUser() throws Exception {
        when(userService.findByEmail(EMAIL)).thenReturn(user(4.0, 7L));

        kafkaConsumer.handleGetUserFromReviewService(EMAIL, "reply-topic");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("reply-topic"), json.capture());
        assertThat(json.getValue())
                .contains("\"email\":\"" + EMAIL + "\"")
                .contains("\"login\":\"reader\"")
                .contains("\"bucks\":7");
    }

    @Test
    void handleGetRatingFromReviewService_firstReview_averagesWithCurrentRating() {
        User user = user(3.0, 0L);
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(user);

        kafkaConsumer.handleGetRatingFromReviewService(EMAIL + ":5.0:0");

        assertThat(user.getRating()).isEqualTo(4.0);
        verify(userService).updateUser(user);
    }

    @Test
    void handleGetRatingFromReviewService_existingReviews_weightedAverageRoundedToOneDecimal() {
        User user = user(4.0, 0L);
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(user);

        // (4.0 * 3 + 1.0) / 4 = 3.25 -> 3.3
        kafkaConsumer.handleGetRatingFromReviewService(EMAIL + ":1.0:3");

        assertThat(user.getRating()).isEqualTo(3.3);
        verify(userService).updateUser(user);
    }

    @Test
    void handleUpdateUserRequest_setsBucks() {
        User user = user(4.0, 10L);
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(user);

        kafkaConsumer.handleUpdateUserRequest(EMAIL + ":42");

        assertThat(user.getBucks()).isEqualTo(42L);
        verify(userService).updateUser(user);
    }

    @Test
    void notificationFlags_areToggledAndSaved() {
        User user = user(4.0, 10L);
        when(userService.findByEmailForCheck(EMAIL)).thenReturn(user);

        kafkaConsumer.handleChangeUserNotificationTrue(EMAIL);
        assertThat(user.isNotifications()).isTrue();

        kafkaConsumer.handleChangeUserNotificationFalse(EMAIL);
        assertThat(user.isNotifications()).isFalse();

        verify(userDAO, times(2)).save(user);
    }
}
