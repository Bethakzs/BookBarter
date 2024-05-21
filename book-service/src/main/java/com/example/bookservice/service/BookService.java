package com.example.bookservice.service;

import com.example.bookservice.dto.BookDTO;
import com.example.bookservice.dto.BookUserDTO;
import com.example.bookservice.entity.Book;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface BookService {

    Book saveBook(BookDTO book, MultipartFile image, String email) throws IOException;

    Optional<Book> getBook(Long id);

    void deleteBook(Long id);

    List<Book> getAllBooksByEmailWithoutUser(String email);

    List<BookUserDTO> getAllBooksByEmail(String email);

    List<BookUserDTO> getAllAvailable();

    Book updateBook(Book book, Long id);

    void save(Book book);

    BookUserDTO getBookById(Long id);
}
