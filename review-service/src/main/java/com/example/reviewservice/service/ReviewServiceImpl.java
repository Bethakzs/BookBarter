package com.example.reviewservice.service;

import com.example.reviewservice.kafka.ReplyProcessor;
import com.example.reviewservice.dao.ReviewDAO;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.dto.UserDTO;
import com.example.reviewservice.entity.Review;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewDAO reviewRepository;
    private final ReplyProcessor replyProcessor;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private UserDTO getUserByEmail(String email) {
        CompletableFuture<String> userFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(email)
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "review-service-response-get-user-by-email-topic")
                .setHeader("serviceName", "review-service")
                .build());
        String userJson = userFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(userJson, UserDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing user", e);
        }
    }

    public List<ReviewDTO> getReviewsByUserEmail(String email) {
        List<Review> reviews = reviewRepository.findAllByRevieweeEmail(email);
        if (reviews.isEmpty()) {
            return List.of();
        }

        UserDTO userReviewer = getUserByEmail(reviews.get(0).getReviewerEmail());
        UserDTO userReviewee = getUserByEmail(reviews.get(0).getRevieweeEmail());

        return reviews.stream().map(review -> ReviewDTO.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .reviewer(userReviewer)
                .reviewee(userReviewee)
                .build()).collect(Collectors.toList());
    }

    public Review createReview(Review review, String email) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        int reviewsCount = reviewRepository.findAllByRevieweeEmail(review.getRevieweeEmail()).size();
        String request = review.getRevieweeEmail() + ":" + review.getRating() + ":" + reviewsCount;
        kafkaTemplate.send("user-service-request-send-rating-topic", request);

        Review newReview = Review.builder()
                .content(review.getContent())
                .rating(review.getRating())
                .reviewerEmail(email)
                .revieweeEmail(review.getRevieweeEmail())
                .build();
        return reviewRepository.save(newReview);
    }

    public void deleteById(Long id) {
        if(!reviewRepository.existsById(id)){
            throw new IllegalArgumentException("Review with id " + id + " not found");
        }
        reviewRepository.deleteById(id);
    }
}
