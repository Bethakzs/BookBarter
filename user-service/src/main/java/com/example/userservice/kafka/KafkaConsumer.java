package com.example.userservice.kafka;

import com.example.userservice.dao.UserDAO;
import com.example.userservice.entity.User;
import com.example.userservice.service.UserServiceImpl;
import com.example.userservice.dto.response.UserReviewDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {

    private final UserServiceImpl userService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserDAO userDAO;

    private UserReviewDTO convertToUserReviewDTO(User user) {
        return UserReviewDTO.builder()
                .login(user.getLogin())
                .email(user.getEmail())
                .pwd(user.getPwd())
                .rating(user.getRating())
                .phone(user.getPhone())
                .image(user.getImage())
                .bucks(user.getBucks())
                .build();
    }

    private void sendUser(String replyTopic, UserReviewDTO userReviewDTO) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String userJson = mapper.writeValueAsString(userReviewDTO);
            kafkaTemplate.send(replyTopic, userJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing user", e);
        }
    }

    // Review
    @KafkaListener(topics = "user-service-request-get-user-by-email-topic", groupId = "user-service")
    @Transactional
    public void handleGetUserFromReviewService(@Payload String email, @Header(KafkaHeaders.REPLY_TOPIC) String replyTopic) throws Exception {
        User user = userService.findByEmail(email);
        UserReviewDTO userReviewDTO = convertToUserReviewDTO(user);
        sendUser(replyTopic, userReviewDTO);
    }

    @KafkaListener(topics = "user-service-request-get-user-by-email-without-exist-topic", groupId = "auth-service")
    @Transactional
    public void handleGetUserFromAuthService(@Payload String email, @Header(KafkaHeaders.REPLY_TOPIC) String replyTopic) {
        User user = userService.findByEmailWithOutCheck(email);
        UserReviewDTO userReviewDTO = convertToUserReviewDTO(user);
        sendUser(replyTopic, userReviewDTO);
    }

    @KafkaListener(topics = "user-service-request-send-rating-topic", groupId = "user-service")
    @Transactional
    public void handleGetRatingFromReviewService(String request) {
        String[] parts = request.split(":");
        String email = parts[0];
        double rating = Double.parseDouble(parts[1]);
        int reviewsCount = Integer.parseInt(parts[2]);
        User user = userService.findByEmailForCheck(email);
        double newRating;
        if(reviewsCount == 0) {
            newRating = (user.getRating() + rating) / 2.;
        } else {
            newRating = (user.getRating() * reviewsCount + rating) / (reviewsCount + 1);
        }
        newRating = Math.round(newRating * 10.0) / 10.0; // Round to one decimal place
        user.setRating(newRating);
        userService.updateUser(user);
    }

    @KafkaListener(topics = "user-service-request-update-user-topic", groupId = "user-service")
    public void handleUpdateUserRequest(String request) {
        String[] parts = request.split(":");
        String email = parts[0];
        long bucks = Long.parseLong(parts[1]);
        User user = userService.findByEmailForCheck(email);
        user.setBucks(bucks);
        userService.updateUser(user);
    }

    // Auth
    @KafkaListener(topics = "user-service-request-save-user", groupId = "user-service")
    public void handleSaveUserRequest(@Payload User user) {
        userService.updateUser(user);
    }

    //Notification
    @KafkaListener(topics = "user-service-request-change-notification-true", groupId = "user-service")
    public void handleChangeUserNotificationTrue(String request) {
        User user = userService.findByEmailForCheck(request);
        user.setNotifications(true);
        userDAO.save(user);
    }
    //Notification
    @KafkaListener(topics = "user-service-request-change-notification-false", groupId = "user-service")
    public void handleChangeUserNotificationFalse(String request) {
        User user = userService.findByEmailForCheck(request);
        user.setNotifications(false);
        userDAO.save(user);
    }
}
