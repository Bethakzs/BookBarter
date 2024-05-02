package com.example.wishlistservice.dto;

import lombok.*;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private Long id;
    private String title;
    private byte[] image;
    private List<Genre> genres;
    private String description;
    private String author;
    private int year;
    private String publishedBy;
    private int price;
    private BookStatus status;
    private String userEmail;
}