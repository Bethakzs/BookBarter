package com.example.reviewservice.service;

import com.example.reviewservice.dao.ReviewDAO;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.entity.Review;
import com.example.reviewservice.kafka.ReplyProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.List;
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
class ReviewServiceImplTest {

    private static final String REVIEWER = "reviewer@bookbarter.com";
    private static final String REVIEWEE = "reviewee@bookbarter.com";

    @Mock
    private ReviewDAO reviewRepository;

    @Mock
    private ReplyProcessor replyProcessor;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private static String userJson(String login, String email) {
        return "{\"login\":\"" + login + "\",\"email\":\"" + email + "\",\"phone\":\"1\",\"rating\":4.5}";
    }

    private static Review review(long id, String content, double rating) {
        return Review.builder().id(id).content(content).rating(rating)
                .reviewerEmail(REVIEWER).revieweeEmail(REVIEWEE).build();
    }

    // ---------- getReviewsByUserEmail ----------

    @Test
    void getReviewsByUserEmail_noReviews_returnsEmptyListWithoutKafkaCalls() {
        when(reviewRepository.findAllByRevieweeEmail(REVIEWEE)).thenReturn(List.of());

        assertThat(reviewService.getReviewsByUserEmail(REVIEWEE)).isEmpty();
        verifyNoInteractions(kafkaTemplate, replyProcessor);
    }

    @Test
    void getReviewsByUserEmail_withReviews_enrichesWithReviewerAndReviewee() {
        when(reviewRepository.findAllByRevieweeEmail(REVIEWEE))
                .thenReturn(List.of(review(1L, "Great trade", 5.0), review(2L, "Slow reply", 3.0)));
        when(replyProcessor.waitForReply()).thenReturn(
                CompletableFuture.completedFuture(userJson("reviewer", REVIEWER)),
                CompletableFuture.completedFuture(userJson("reviewee", REVIEWEE)));

        List<ReviewDTO> result = reviewService.getReviewsByUserEmail(REVIEWEE);

        assertThat(result).extracting(ReviewDTO::getId).containsExactly(1L, 2L);
        assertThat(result).extracting(ReviewDTO::getContent).containsExactly("Great trade", "Slow reply");
        assertThat(result).extracting(ReviewDTO::getRating).containsExactly(5.0, 3.0);
        assertThat(result.get(0).getReviewer().getEmail()).isEqualTo(REVIEWER);
        assertThat(result.get(0).getReviewee().getLogin()).isEqualTo("reviewee");

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(2)).send(messages.capture());
        assertThat(messages.getAllValues()).extracting(Message::getPayload).containsExactly(REVIEWER, REVIEWEE);
        assertThat(messages.getValue().getHeaders().get(KafkaHeaders.REPLY_TOPIC))
                .isEqualTo("review-service-response-get-user-by-email-topic");
    }

    @Test
    void getReviewsByUserEmail_malformedUserReply_throws() {
        when(reviewRepository.findAllByRevieweeEmail(REVIEWEE)).thenReturn(List.of(review(1L, "ok", 4.0)));
        when(replyProcessor.waitForReply()).thenReturn(CompletableFuture.completedFuture("not-json"));

        assertThatThrownBy(() -> reviewService.getReviewsByUserEmail(REVIEWEE))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error deserializing user");
    }

    // ---------- createReview ----------

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 5.5, 10.0})
    void createReview_ratingOutOfRange_isRejected(double rating) {
        Review input = Review.builder().content("x").rating(rating).revieweeEmail(REVIEWEE).build();

        assertThatThrownBy(() -> reviewService.createReview(input, REVIEWER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rating must be between 1 and 5");
        verifyNoInteractions(kafkaTemplate, reviewRepository);
    }

    @Test
    void createReview_validRating_publishesRatingUpdateAndSavesReview() {
        Review input = Review.builder().content("Nice!").rating(4.0).revieweeEmail(REVIEWEE)
                .reviewerEmail("spoofed@bookbarter.com").build();
        when(reviewRepository.findAllByRevieweeEmail(REVIEWEE))
                .thenReturn(List.of(review(1L, "a", 5.0), review(2L, "b", 3.0)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review saved = reviewService.createReview(input, REVIEWER);

        verify(kafkaTemplate).send("user-service-request-send-rating-topic", REVIEWEE + ":4.0:2");
        assertThat(saved.getId()).isNull();
        assertThat(saved.getContent()).isEqualTo("Nice!");
        assertThat(saved.getRating()).isEqualTo(4.0);
        assertThat(saved.getRevieweeEmail()).isEqualTo(REVIEWEE);
        // reviewer is always taken from the authenticated email, never from the request body
        assertThat(saved.getReviewerEmail()).isEqualTo(REVIEWER);
    }

    // ---------- deleteById ----------

    @Test
    void deleteById_existingReview_isDeleted() {
        when(reviewRepository.existsById(3L)).thenReturn(true);

        reviewService.deleteById(3L);

        verify(reviewRepository).deleteById(3L);
    }

    @Test
    void deleteById_missingReview_throws() {
        when(reviewRepository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.deleteById(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Review with id 3 not found");
        verify(reviewRepository, never()).deleteById(any());
    }
}
