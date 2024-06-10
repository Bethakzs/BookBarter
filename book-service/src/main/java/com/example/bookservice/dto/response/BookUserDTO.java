package com.example.bookservice.dto.response;


import com.example.bookservice.dto.user.UserDTO;
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