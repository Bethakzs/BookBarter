package com.example.reviewservice.service;

import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.entity.Review;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ReviewService {

    List<ReviewDTO> getReviewsByUserEmail(String email);

    Review createReview(Review review, String email);

    void deleteById(Long id);
}
