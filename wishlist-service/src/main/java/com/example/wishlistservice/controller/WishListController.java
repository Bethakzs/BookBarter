package com.example.wishlistservice.controller;

import com.example.wishlistservice.dto.user.BookUserDTO;
import com.example.wishlistservice.service.WishListService;
import com.example.wishlistservice.util.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishListController {

    private final WishListService wishListService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/add/{id}")
    @Transactional
    public void addBookToWishList(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        wishListService.addBookToWishList(email, id);
    }

    @GetMapping("")
    @Transactional
    public List<BookUserDTO> getWishList(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return wishListService.getWishListByEmail(email);
    }

    @DeleteMapping("/delete/{id}")
    @Transactional
    public void removeBookById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        wishListService.removeBookById(email, id);
    }

    @DeleteMapping("/clear")
    @Transactional
    public void clearWishList(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        wishListService.clearWishList(email);
    }
}
