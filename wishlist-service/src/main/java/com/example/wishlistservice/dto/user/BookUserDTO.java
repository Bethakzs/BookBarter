package com.example.wishlistservice.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookUserDTO {
    private Long id;
    private String title;
    private byte[] image;
    private List<String> genres;
    private String description;
    private String author;
    private int year;
    private String publishedBy;
    private int price;
    private UserDTO user;
}