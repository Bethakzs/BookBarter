package com.example.bookservice.dto;


import com.example.bookservice.entity.Genre;
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
//public class BookDTO {
//    private Long id;
//    private String title;
//    private List<Genre> genres;
//    private String description;
//    private String author;
//    private int year;
//    private String publishedBy;
//    private int price;
//}