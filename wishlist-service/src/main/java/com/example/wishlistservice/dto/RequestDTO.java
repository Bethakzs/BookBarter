package com.example.wishlistservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestDTO {
    private String email;
    private Long bookId;
}
