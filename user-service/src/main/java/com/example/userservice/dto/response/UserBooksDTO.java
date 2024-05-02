package com.example.userservice.dto.response;

import com.example.userservice.dto.UserDTO;
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
public class UserBooksDTO {
    private UserDTO user;
    private List<BookDTO> books;
}
