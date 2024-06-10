package com.example.wishlistservice.service;

import com.example.wishlistservice.dao.WishListRepository;
import com.example.wishlistservice.dto.*;
import com.example.wishlistservice.dto.book.BookDTO;
import com.example.wishlistservice.dto.user.BookUserDTO;
import com.example.wishlistservice.dto.user.UserDTO;
import com.example.wishlistservice.entity.WishList;
import com.example.wishlistservice.error.ResourceNotFoundException;
import com.example.wishlistservice.kafka.ReplyProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class WishListServiceImpl implements WishListService {

    private final WishListRepository wishListRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReplyProcessor replyProcessor;

    private UserDTO getUserByEmail(String email) {
        CompletableFuture<String> userFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(email)
                .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "wishlist-service-response-get-user-by-email-topic")
                .setHeader("serviceName", "wishlist-service")
                .build());
        String userJson = userFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(userJson, UserDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing user", e);
        }
    }

    private BookDTO getBookById(Long bookId) {
        CompletableFuture<String> bookFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(bookId.toString())
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-book-by-id-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "wishlist-service-response-get-book-by-id-topic")
                .setHeader("serviceName", "wishlist-service")
                .build());
        String bookJson = bookFuture.join();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(bookJson, BookDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing book", e);
        }
    }

    public WishList addBookToWishList(String email, Long id) {
        WishList wishList = wishListRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    WishList newWishList = new WishList();
                    newWishList.setUserEmail(email);
                    newWishList.setBooksId(new ArrayList<>());
                    return newWishList;
                });

        CompletableFuture<String> bookExistFuture = replyProcessor.waitForReply();
        kafkaTemplate.send(MessageBuilder.withPayload(id.toString())
                .setHeader(KafkaHeaders.TOPIC, "book-service-request-check-exist-book-topic")
                .setHeader(KafkaHeaders.REPLY_TOPIC, "wishlist-service-response-check-topic")
                .setHeader("serviceName", "wishlist-service")
                .build());
        String bookExists;
        try {
            bookExists = bookExistFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if ("true".equalsIgnoreCase(bookExists)) {
            if (!wishList.getBooksId().contains(id)) {
                wishList.getBooksId().add(id);
                return wishListRepository.save(wishList);
            }
            else {
                throw new IllegalArgumentException("Book already exists in wishlist");
            }
        } else {
            throw new IllegalArgumentException("Book not found");
        }
    }

    public List<BookUserDTO> getWishListByEmail(String email) {
        WishList wishList = wishListRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    WishList newWishList = new WishList();
                    newWishList.setUserEmail(email);
                    newWishList.setBooksId(new ArrayList<>());
                    return newWishList;
                });

        List<BookUserDTO> booksWithUsers = new ArrayList<>();
        for (Long bookId : wishList.getBooksId()) {
            BookDTO book = getBookById(bookId);
            UserDTO userReviewer = getUserByEmail(book.getUserEmail());
            booksWithUsers.add(BookUserDTO.builder()
                    .id(book.getId())
                    .title(book.getTitle())
                    .image(book.getImage())
                    .genres(book.getGenres())
                    .description(book.getDescription())
                    .author(book.getAuthor())
                    .year(book.getYear())
                    .publishedBy(book.getPublishedBy())
                    .price(book.getPrice())
                    .user(userReviewer)
                    .build());
        }
        return booksWithUsers;
    }

    public void clearWishList(String email) {
        List<WishList> wishLists = wishListRepository.findAllByUserEmail(email);
        if (!wishLists.isEmpty()) {
            for (WishList wishList : wishLists) {
                wishList.getBooksId().clear();
                wishListRepository.save(wishList);
            }
        } else {
            throw new ResourceNotFoundException("Wishlist not found with email : " + email);
        }
    }

    public void removeBookById(String email, Long id) {
        WishList wishList = wishListRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found with email : " + email));
        if (wishList.getBooksId().contains(id)) {
            wishList.getBooksId().remove(id);
            wishListRepository.save(wishList);
        } else {
            throw new ResourceNotFoundException("Book not found in wishlist");
        }
    }
}
