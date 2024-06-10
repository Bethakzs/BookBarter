package com.example.bookservice.entity;

import com.example.bookservice.entity.genres.GenreInterface;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "books")
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @ElementCollection(targetClass = String.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "book_genre")
    @Column(name = "genre", length = 50)
    private List<String> genres;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "published_by", nullable = false, length = 150)
    private String publishedBy;

    @Column(name = "price", nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookStatus status;

    @Column(name = "user_email")
    private String userEmail;
}