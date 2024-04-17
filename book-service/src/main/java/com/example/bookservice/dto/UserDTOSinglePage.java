package com.example.bookservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTOSinglePage {
    private String login;
    private String email;
    private Double rating;
    private String phone;
    private byte[] image;
}

