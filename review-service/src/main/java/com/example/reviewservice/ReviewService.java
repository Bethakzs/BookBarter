package com.example.reviewservice;

import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.dto.UserDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewDAO reviewRepository;

    @Autowired
    private ReplyProcessor replyProcessor;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public List<ReviewDTO> getReviewsByUserEmail(String email) {
        List<Review> reviews = reviewRepository.findAllByRevieweeEmail(email);

        if (reviews.isEmpty()) {
            return new ArrayList<>();
        }

        CompletableFuture<String> userFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(reviews.get(0).getReviewerEmail())
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "review-service-response-get-user-by-email-topic")
                .setHeader("serviceName", "review-service")
                .build());
        String userJson = userFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        UserDTO userReviewer;
        try {
            userReviewer = mapper.readValue(userJson, UserDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing user", e);
        }

        userFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(reviews.get(0).getRevieweeEmail())
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "review-service-response-get-user-by-email-topic")
                .setHeader("serviceName", "review-service")
                .build());
        userJson = userFuture.join();
        UserDTO userReviewee;
        try {
            userReviewee = mapper.readValue(userJson, UserDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing user", e);
        }

        return reviews.stream().map(review -> ReviewDTO.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .reviewer(userReviewer)
                .reviewee(userReviewee)
                .build()).toList();
    }

    public Review createReview(Review review, String email) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // replace  | format string request = email:rating:count |
        int reviewsCount = reviewRepository.findAllByRevieweeEmail(review.getRevieweeEmail()).size();
        String request = review.getRevieweeEmail() + ":" + review.getRating() + ":" + reviewsCount;
        System.out.println(request);
        kafkaTemplate.send("user-service-request-send-rating-topic", request);
        // replace

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