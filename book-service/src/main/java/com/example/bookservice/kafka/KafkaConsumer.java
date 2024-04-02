package com.example.bookservice.kafka;

import com.example.bookservice.entity.Book;
import com.example.bookservice.service.BookService;
import com.example.bookservice.entity.BookStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BookService bookService;

    @KafkaListener(topics = "book-service-request-check-exist-book-topic", groupId = "book-service")
    public void processRequestCheck(ConsumerRecord<String, String> request) {
        Long bookId = Long.parseLong(request.value());
        Optional<Book> book = bookService.getBook(bookId);
        String exists = book.isPresent() ? "true" : "false";
        kafkaTemplate.send(MessageBuilder.withPayload(exists)
                .setHeader(KafkaHeaders.TOPIC, "wishlist-service-response-check-topic")
                .setHeader("serviceName", "book-service")
                .build());
    }

    @Transactional
    @KafkaListener(topics = "book-service-request-get-book-by-id-topic", groupId = "book-service")
    public void processRequestGetBook(ConsumerRecord<String, String> request, @Header(KafkaHeaders.REPLY_TOPIC) String replyTopic) {
        Long bookId = Long.parseLong(request.value());
        Optional<Book> bookOptional = bookService.getBook(bookId);
        if (bookOptional.isPresent()) {
            Book book = bookOptional.get();
            ObjectMapper objectMapper = new ObjectMapper();
            System.out.println("Sending book: " + replyTopic);
            try {
                String bookJson = objectMapper.writeValueAsString(book);
                kafkaTemplate.send(replyTopic, bookJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing JSON", e);
            }
        } else {
            kafkaTemplate.send(replyTopic, "null");
        }
    }

    @KafkaListener(topics = "book-service-request-update-book-topic", groupId = "book-service")
    public void handleUpdateBookRequest(String request) {
        System.out.println("Received update book request: " + request);
        String[] parts = request.split(":");
        Long bookId = Long.parseLong(parts[0]);
        String status = parts[1];
        Book book = bookService.getBook(bookId).orElseThrow(() -> new RuntimeException("Book not found"));
        book.setStatus(BookStatus.valueOf(status));
        bookService.save(book);
    }
}
