package com.example.purchaseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    Long id;
    String title;
    private byte[] image;
    private List<Genre> genres;
    String description;
    String author;
    int year;
    String publishedBy;
    int price;
    BookStatus status;
    String userEmail;
}