package com.example.authservice.dto;

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

    public static Role valueOf(int value) {
        for (Role role : Role.values()) {
            if (role.getValue() == value) {
                return role;
            }
        }
        throw new IllegalArgumentException("No Role with value " + value);
    }
}