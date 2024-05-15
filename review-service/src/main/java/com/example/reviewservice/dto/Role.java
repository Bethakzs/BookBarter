package com.example.reviewservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum Role {
    ROLE_USER(2001),
    ROLE_ADMIN(5320),
    ROLE_EDITOR(1808);

    private int value;
}

