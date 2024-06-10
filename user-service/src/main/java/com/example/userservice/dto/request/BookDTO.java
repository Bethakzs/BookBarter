package com.example.userservice.dto.request;

import com.example.userservice.dto.book.BookStatus;
import lombok.Data;

import java.util.List;

@Data
public class BookDTO {
    private Long id;
    private String title;
    private byte[] image;
    private List<String> genres;
    private String description;
    private String author;
    private int year;
    private BookStatus status;
    private String publishedBy;
    private int price;
    private String userEmail;
}