package com.example.reviewservice.dao;

import com.example.reviewservice.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewDAO extends JpaRepository<Review, Long> {
    List<Review> findAllByRevieweeEmail(String email);
}
