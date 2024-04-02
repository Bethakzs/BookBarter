package com.example.userservice;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "username", nullable = false, length = 100)
    String login;

    @Column(name = "email", nullable = false, unique = true, length = 110)
    String email;

    @Column(name = "password", nullable = false, length = 100)
    String pwd;

    @Column(name = "mobile_phone", nullable = false)
    String phone;

    @Column(name = "rating", nullable = false)
    Double rating;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "buck", nullable = false)
    Long buck;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    Role role;

    @Column(name = "refresh_token")
    String refreshToken;
}