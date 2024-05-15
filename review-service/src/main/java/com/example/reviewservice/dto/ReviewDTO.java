package com.example.reviewservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewDTO {
    private Long id;
    private String content;
    private Double rating;
    private UserDTO reviewer;
    private UserDTO reviewee;
}