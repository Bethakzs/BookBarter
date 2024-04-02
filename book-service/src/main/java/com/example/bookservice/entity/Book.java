package com.example.bookservice.entity;

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
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "books")
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "title", nullable = false)
    String title;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @ElementCollection(targetClass = Genre.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "book_genre")
    @Column(name = "genre", length = 50)
    private List<Genre> genres;

    @Column(name = "description", nullable = false, length = 500)
    String description;

    @Column(name = "author", nullable = false, length = 100)
    String author;

    @Column(name = "year", nullable = false)
    int year;

    @Column(name = "published_by", nullable = false, length = 150)
    String publishedBy;

    @Column(name = "price", nullable = false)
    int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    BookStatus status;

    @Column(name = "user_email")
    String userEmail;
}