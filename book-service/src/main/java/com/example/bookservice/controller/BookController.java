package com.example.bookservice.controller;


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
import java.util.List;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping("/publish/{email}")
    @Transactional
    public Book addBook(@RequestParam("book") String bookStr, @RequestParam("image") MultipartFile image, @PathVariable String email) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        BookDTO bookDTO = objectMapper.readValue(bookStr, BookDTO.class);
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

    @GetMapping("/get-email/{email}")
    @Transactional
    public ResponseEntity<?> getBooksByEmail(@PathVariable String email) {
        return ResponseEntity.ok(bookService.getAllBooksByEmail(email));
    }


    @GetMapping("/get-all")
    @Transactional
    public List<BookUserDTO> getAllBooks() {
        return bookService.getAllAvailable();
    }
}
