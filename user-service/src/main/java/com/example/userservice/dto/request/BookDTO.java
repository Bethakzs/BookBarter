package com.example.userservice.dto.request;

import com.example.userservice.dto.BookStatus;
import com.example.userservice.dto.Genre;
import lombok.Data;

import java.util.List;

@Data
public class BookDTO {
    private Long id;
    private String title;
    private byte[] image;
    private List<Genre> genres;
    private String description;
    private String author;
    private int year;
    private BookStatus status;
    private String publishedBy;
    private int price;
    private String userEmail;
}