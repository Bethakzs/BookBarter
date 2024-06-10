package com.example.wishlistservice.service;

import com.example.wishlistservice.dto.user.BookUserDTO;
import com.example.wishlistservice.entity.WishList;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WishListService {

    WishList addBookToWishList(String email, Long id);

    List<BookUserDTO> getWishListByEmail(String email);

    void clearWishList(String email);

    void removeBookById(String email, Long id);
}
