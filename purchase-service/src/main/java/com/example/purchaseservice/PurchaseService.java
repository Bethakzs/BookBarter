package com.example.purchaseservice;

import com.example.purchaseservice.dto.BookDTO;
import com.example.purchaseservice.dto.BookStatus;
import com.example.purchaseservice.dto.PurchaseDTO;
import com.example.purchaseservice.dto.UserDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PurchaseService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ReplyProcessor replyProcessor;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void buyBook(Purchase purchase) {
        CompletableFuture<String> userFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(purchase.getBuyerEmail())
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "purchase-service-response-get-user-by-email-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        String userJson = userFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        UserDTO buyer;
        try {
            buyer = mapper.readValue(userJson, UserDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing user", e);
        }

        CompletableFuture<String> bookFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(purchase.getBookId().toString())
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-book-by-id-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "purchase-service-response-get-book-by-id-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        String bookJson = bookFuture.join();
        BookDTO book;
        try {
            book = mapper.readValue(bookJson, BookDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing book", e);
        }

        if (buyer.getBuck() < book.getPrice()) {
            throw new RuntimeException("Not enough bucks");
        }
        if (!book.getStatus().equals(BookStatus.AVAILABLE)) {
            throw new RuntimeException("Book is not available");
        }

        String requestBook = book.getId() + ":" + BookStatus.RESERVED;
        kafkaTemplate.send(MessageBuilder.withPayload(requestBook)
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-update-book-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        String requestUser = buyer.getEmail() + ":" + (buyer.getBuck() - book.getPrice());
        kafkaTemplate.send(MessageBuilder.withPayload(requestUser)
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-update-user-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        purchase.setStatus(PurchaseStatus.RESERVED);
        purchaseRepository.save(purchase);
    }

    public void confirmPurchase(Long id, String email) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new RuntimeException("Purchase not found"));
        if (!purchase.getStatus().equals(PurchaseStatus.RESERVED)) {
            throw new RuntimeException("Purchase is not reserved");
        }
        if (!purchase.getBuyerEmail().equals(email)) {
            throw new RuntimeException("You are not the buyer");
        }
        System.out.println(purchase.getBuyerEmail());

        CompletableFuture<String> userFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(purchase.getSellerEmail())
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "purchase-service-response-get-user-by-email-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        String userJson = userFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        UserDTO seller;
        try {
            seller = mapper.readValue(userJson, UserDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing user", e);
        }

        CompletableFuture<String> bookFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(purchase.getBookId().toString())
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-book-by-id-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "purchase-service-response-get-book-by-id-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        String bookJson = bookFuture.join();
        BookDTO book;
        try {
            book = mapper.readValue(bookJson, BookDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing book", e);
        }

        String requestBook = book.getId() + ":" + BookStatus.SOLD;
        kafkaTemplate.send(MessageBuilder.withPayload(requestBook)
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-update-book-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        System.out.println(seller.getEmail() + ":" + (seller.getBuck() + book.getPrice()));
        String requestUser = seller.getEmail() + ":" + (seller.getBuck() + book.getPrice());
        kafkaTemplate.send(MessageBuilder.withPayload(requestUser)
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-update-user-topic")
                .setHeader("serviceName", "purchase-service")
                .build());
        purchase.setStatus(PurchaseStatus.CONFIRMED);
        purchaseRepository.save(purchase);
    }
}
