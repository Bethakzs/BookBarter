package com.example.bookservice.service;

import com.example.bookservice.dao.BookDAO;
import com.example.bookservice.entity.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookDAO bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    public void testGetBook() {
        Book book = new Book();
        book.setTitle("Test Book");
        when(bookRepository.findById(any(Long.class))).thenReturn(Optional.of(book));

        Optional<Book> result = bookService.getBook(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Book", result.get().getTitle());
    }

    @Test
    public void testDeleteBook() {
        doNothing().when(bookRepository).deleteById(any(Long.class));

        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    public void testGetAllBooksByEmailWithoutUser() {
        List<Book> books = new ArrayList<>();
        Book book = new Book();
        book.setTitle("Test Book");
        books.add(book);
        when(bookRepository.findByUserEmail(anyString())).thenReturn(books);

        List<Book> result = bookService.getAllBooksByEmailWithoutUser("test@example.com");

        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
    }

    @Test
    public void testUpdateBook() {
        Book book = new Book();
        book.setTitle("Test Book");
        when(bookRepository.findById(any(Long.class))).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        Book result = bookService.updateBook(book, 1L);

        assertEquals("Test Book", result.getTitle());
    }

    @Test
    public void testSave() {
        Book book = new Book();
        book.setTitle("Test Book");
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        bookService.save(book);

        verify(bookRepository).save(book);
    }
}
