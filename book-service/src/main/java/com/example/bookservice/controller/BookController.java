package com.example.bookservice.controller;


import com.example.bookservice.JwtTokenProvider;
import com.example.bookservice.entity.Genre;
import com.example.bookservice.service.BookService;
import com.example.bookservice.dto.BookDTO;
import com.example.bookservice.dto.BookUserDTO;
import com.example.bookservice.entity.Book;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/publish")
    @Transactional
    public Book addBook(
            @RequestParam("title") String title,
            @RequestParam("genres") List<String> genres,
            @RequestParam("description") String description,
            @RequestParam("author") String author,
            @RequestParam("year") String year,
            @RequestParam("publishedBy") String publishedBy,
            @RequestParam("price") String price,
            @RequestParam("image") MultipartFile image,
            @RequestHeader("Authorization") String authHeader
    ) throws IOException {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);

        BookDTO bookDTO = BookDTO.builder()
                .title(title)
                .genres(genres)
                .description(description)
                .author(author)
                .year(year)
                .publishedBy(publishedBy)
                .price(price)
                .build();
        return bookService.saveBook(bookDTO, image, email);
    }

    @PutMapping("/edit/{id}")
    public Book updateBook(@PathVariable Long id, @RequestParam("book") String bookStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Book bookDetails = objectMapper.readValue(bookStr, Book.class);
        return bookService.updateBook(bookDetails, id);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        if(bookService.getBook(id).isEmpty()) {
            return ResponseEntity.badRequest().body("Book not found");
        }
        bookService.deleteBook(id);
        return ResponseEntity.ok("Book removed ! " + id);
    }

    @GetMapping("/get-email")
    @Transactional
    public ResponseEntity<?> getBooksByEmail(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return ResponseEntity.ok(bookService.getAllBooksByEmail(email));
    }

    @GetMapping("/get-all")
    @Transactional
    public List<BookUserDTO> getAllBooks() {
        return bookService.getAllAvailable();
    }
}
