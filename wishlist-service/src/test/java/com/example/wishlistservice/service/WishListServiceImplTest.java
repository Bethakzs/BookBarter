package com.example.wishlistservice.service;

import com.example.wishlistservice.dao.WishListRepository;
import com.example.wishlistservice.dto.user.BookUserDTO;
import com.example.wishlistservice.entity.WishList;
import com.example.wishlistservice.error.ResourceNotFoundException;
import com.example.wishlistservice.kafka.ReplyProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishListServiceImplTest {

    private static final String EMAIL = "reader@bookbarter.com";
    private static final String OWNER = "owner@bookbarter.com";

    @Mock
    private WishListRepository wishListRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ReplyProcessor replyProcessor;

    @InjectMocks
    private WishListServiceImpl wishListService;

    private static WishList wishList(Long... bookIds) {
        return WishList.builder().id(1L).userEmail(EMAIL).booksId(new ArrayList<>(List.of(bookIds))).build();
    }

    private static CompletableFuture<String> reply(String value) {
        return CompletableFuture.completedFuture(value);
    }

    private static String bookJson(long id, String title, int price) {
        return "{\"id\":" + id + ",\"title\":\"" + title + "\",\"genres\":[\"Novel\"],\"author\":\"A\",\"year\":1999,"
                + "\"price\":" + price + ",\"status\":\"AVAILABLE\",\"userEmail\":\"" + OWNER + "\"}";
    }

    private static String userJson() {
        return "{\"login\":\"owner\",\"email\":\"" + OWNER + "\",\"rating\":4.0,\"phone\":\"1\",\"bucks\":3}";
    }

    // ---------- addBookToWishList ----------

    @Test
    void addBookToWishList_noWishListYet_createsOneWithTheBook() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());
        when(replyProcessor.waitForReply()).thenReturn(reply("true"));
        when(wishListRepository.save(any(WishList.class))).thenAnswer(inv -> inv.getArgument(0));

        WishList result = wishListService.addBookToWishList(EMAIL, 42L);

        assertThat(result.getUserEmail()).isEqualTo(EMAIL);
        assertThat(result.getBooksId()).containsExactly(42L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(message.capture());
        assertThat(message.getValue().getPayload()).isEqualTo("42");
        assertThat(message.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("book-service-request-check-exist-book-topic");
    }

    @Test
    void addBookToWishList_existingWishList_appendsBook() {
        WishList existing = wishList(1L);
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(replyProcessor.waitForReply()).thenReturn(reply("TRUE"));
        when(wishListRepository.save(existing)).thenReturn(existing);

        WishList result = wishListService.addBookToWishList(EMAIL, 2L);

        assertThat(result.getBooksId()).containsExactly(1L, 2L);
    }

    @Test
    void addBookToWishList_bookAlreadyInWishList_throws() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(wishList(7L)));
        when(replyProcessor.waitForReply()).thenReturn(reply("true"));

        assertThatThrownBy(() -> wishListService.addBookToWishList(EMAIL, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book already exists in wishlist");
        verify(wishListRepository, never()).save(any());
    }

    @Test
    void addBookToWishList_bookDoesNotExist_throws() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(wishList()));
        when(replyProcessor.waitForReply()).thenReturn(reply("false"));

        assertThatThrownBy(() -> wishListService.addBookToWishList(EMAIL, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book not found");
        verify(wishListRepository, never()).save(any());
    }

    // ---------- getWishListByEmail ----------

    @Test
    void getWishListByEmail_resolvesEachBookAndItsOwner() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(wishList(10L, 11L)));
        when(replyProcessor.waitForReply()).thenReturn(
                reply(bookJson(10, "Dune", 4)), reply(userJson()),
                reply(bookJson(11, "Emma", 2)), reply(userJson()));

        List<BookUserDTO> result = wishListService.getWishListByEmail(EMAIL);

        assertThat(result).extracting(BookUserDTO::getId).containsExactly(10L, 11L);
        assertThat(result).extracting(BookUserDTO::getTitle).containsExactly("Dune", "Emma");
        assertThat(result).extracting(BookUserDTO::getPrice).containsExactly(4, 2);
        assertThat(result.get(0).getGenres()).containsExactly("Novel");
        assertThat(result.get(0).getUser().getEmail()).isEqualTo(OWNER);
        assertThat(result.get(1).getUser().getLogin()).isEqualTo("owner");
        verify(kafkaTemplate, times(4)).send(any(Message.class));
    }

    @Test
    void getWishListByEmail_noWishList_returnsEmptyList() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(wishListService.getWishListByEmail(EMAIL)).isEmpty();
        verifyNoInteractions(kafkaTemplate, replyProcessor);
    }

    @Test
    void getWishListByEmail_malformedBookReply_throws() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(wishList(10L)));
        when(replyProcessor.waitForReply()).thenReturn(reply("not-json"));

        assertThatThrownBy(() -> wishListService.getWishListByEmail(EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error deserializing book");
    }

    // ---------- clearWishList ----------

    @Test
    void clearWishList_existing_clearsAllBooks() {
        WishList first = wishList(1L, 2L);
        WishList second = wishList(3L);
        when(wishListRepository.findAllByUserEmail(EMAIL)).thenReturn(List.of(first, second));

        wishListService.clearWishList(EMAIL);

        assertThat(first.getBooksId()).isEmpty();
        assertThat(second.getBooksId()).isEmpty();
        verify(wishListRepository).save(first);
        verify(wishListRepository).save(second);
    }

    @Test
    void clearWishList_missing_throwsNotFound() {
        when(wishListRepository.findAllByUserEmail(EMAIL)).thenReturn(List.of());

        assertThatThrownBy(() -> wishListService.clearWishList(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(EMAIL);
    }

    // ---------- removeBookById ----------

    @Test
    void removeBookById_presentBook_removesByValueNotIndex() {
        WishList existing = wishList(1L, 2L, 3L);
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(existing));

        wishListService.removeBookById(EMAIL, 2L);

        assertThat(existing.getBooksId()).containsExactly(1L, 3L);
        verify(wishListRepository).save(existing);
    }

    @Test
    void removeBookById_bookNotInWishList_throwsNotFound() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.of(wishList(1L)));

        assertThatThrownBy(() -> wishListService.removeBookById(EMAIL, 9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book not found in wishlist");
        verify(wishListRepository, never()).save(any());
    }

    @Test
    void removeBookById_noWishList_throwsNotFound() {
        when(wishListRepository.findByUserEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishListService.removeBookById(EMAIL, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(EMAIL);
    }
}
