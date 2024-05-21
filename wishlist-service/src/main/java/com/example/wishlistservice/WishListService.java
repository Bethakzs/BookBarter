package com.example.wishlistservice;

import com.example.wishlistservice.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class WishListService {

    @Autowired
    private WishListRepository wishListRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ReplyProcessor replyProcessor;

    public ResponseEntity<?> addBookToWishList(String email, Long id) {
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
                return ResponseEntity.ok(wishListRepository.save(wishList));
            }
            else {
                return ResponseEntity.badRequest().body("Book already exists in wishlist");
            }
        } else {
            return ResponseEntity.badRequest().body("Book not found");
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
        List<BookDTO> books = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (Long bookId : wishList.getBooksId()) {
            CompletableFuture<String> bookFuture = replyProcessor.waitForReply();
            kafkaTemplate.send(MessageBuilder.withPayload(bookId.toString())
                    .setHeader(KafkaHeaders.TOPIC, "book-service-request-get-book-by-id-topic")
                    .setHeader(KafkaHeaders.REPLY_TOPIC, "wishlist-service-response-get-book-by-id-topic")
                    .setHeader("serviceName", "wishlist-service")
                    .build());
            String bookJson = bookFuture.join();
            try {
                BookDTO book = mapper.readValue(bookJson, BookDTO.class);
                books.add(book);

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing book", e);
            }
        }

        for (BookDTO book : books) {
            CompletableFuture<String> userFuture = replyProcessor.waitForReply();
            kafkaTemplate.send(MessageBuilder.withPayload(book.getUserEmail())
                    .setHeader(KafkaHeaders.TOPIC, "user-service-request-get-user-by-email-topic")
                    .setHeader(KafkaHeaders.REPLY_TOPIC, "wishlist-service-response-get-user-by-email-topic")
                    .setHeader("serviceName", "wishlist-service")
                    .build());
            String userJson = userFuture.join();
            UserDTO userDTO;
            try {
                userDTO = mapper.readValue(userJson, UserDTO.class);
                System.out.println(userDTO.getEmail());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing user", e);
            }

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
                    .user(userDTO)
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
