package com.example.authservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum Role {
    ROLE_USER(1, 2001),
    ROLE_ADMIN(2, 5320),
    ROLE_EDITOR(3, 1808);

    private int id;
    private int value;
}