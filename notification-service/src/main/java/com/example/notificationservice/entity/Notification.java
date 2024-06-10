package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notifications")
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "email", nullable = false)
    String email;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "login", nullable = false)
    String login;

    @Column(name = "phone", nullable = false)
    String phone;

    @Column(name = "rating", nullable = false)
    Double rating;

    @Column(name = "price", nullable = false)
    int price;

}