package com.example.reviewservice.controller;

import com.example.reviewservice.service.ReviewService;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.entity.Review;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/add/{email}")
    @Transactional
    public ResponseEntity<Review> createReview(@RequestBody Review review, @PathVariable String email) {
        return ResponseEntity.ok(reviewService.createReview(review, email));
    }

    @GetMapping("/get/{email}")
    @Transactional
    public ResponseEntity<List<ReviewDTO>> getReviewsByUserEmail(@PathVariable String email) {
        return ResponseEntity.ok(reviewService.getReviewsByUserEmail(email));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> getReviewsByUserEmail(@PathVariable Long id) {
        reviewService.deleteById(id);
        return ResponseEntity.ok("Review deleted successfully!");
    }
}
