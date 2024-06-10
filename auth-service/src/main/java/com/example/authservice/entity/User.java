package com.example.authservice.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String login;

    @Column(name = "email", nullable = false, unique = true, length = 110)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String pwd;

    @Column(name = "mobile_phone", unique = true, nullable = false)
    private String phone;

    @Column(name = "rating", nullable = false)
    private Double rating;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "buck", nullable = false)
    private Long bucks;

    @ElementCollection(targetClass = Role.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles")
    @Column(name = "role", nullable = false, length = 20)
    private Set<Role> roles;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "notifications")
    private boolean notifications;
}