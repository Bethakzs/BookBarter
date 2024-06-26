package com.example.bookservice.dto.request;


import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private String id;
    private String title;
    private List<String> genres;
    private String description;
    private String author;
    private String year;
    private String publishedBy;
    private String price;
}