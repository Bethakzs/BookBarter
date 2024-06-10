package com.example.wishlistservice.dto.request;

import com.example.wishlistservice.dto.book.BookDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishListDTO {
    List<BookDTO> books;
}
