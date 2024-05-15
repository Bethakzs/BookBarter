package com.example.userservice.dto;

import com.example.userservice.dto.request.BookDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String login;
    private String email;
    private String phone;
    private Double rating;
    private byte[] image;
    private Long bucks;
    private boolean notifications;
    private List<BookDTO> books;
}
