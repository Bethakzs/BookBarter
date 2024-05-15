package com.example.reviewservice;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "reviews")
@Builder
public class Review {

    @Id    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "content", nullable = false, length = 500)
    String content;

    @Column(name = "rating", nullable = false)
    Double rating;

    @Column(name = "reviewer_email", nullable = false)
    String reviewerEmail;

    @Column(name = "reviewee_email", nullable = false)
    String revieweeEmail;
}