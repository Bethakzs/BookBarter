package com.example.wishlistservice;

import com.example.wishlistservice.dto.BookDTO;
import com.example.wishlistservice.dto.BookUserDTO;
import com.example.wishlistservice.dto.RequestDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishListController {

    private final WishListService wishListService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/add/{id}")
    @Transactional
    public ResponseEntity<?> addBookToWishList(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return wishListService.addBookToWishList(email, id);
    }

    @GetMapping("")
    @Transactional
    public ResponseEntity<?> getWishList(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        List<BookUserDTO> books = wishListService.getWishListByEmail(email);
        return ResponseEntity.ok(books);
    }

    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<Void> removeBookById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        wishListService.removeBookById(email, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<Void> clearWishList(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        wishListService.clearWishList(email);
        return ResponseEntity.noContent().build();
    }
}
