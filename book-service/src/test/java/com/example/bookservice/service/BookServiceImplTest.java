package com.example.bookservice.service;

import com.example.bookservice.dao.BookDAO;
import com.example.bookservice.dto.request.BookDTO;
import com.example.bookservice.dto.response.BookUserDTO;
import com.example.bookservice.entity.Book;
import com.example.bookservice.entity.BookStatus;
import com.example.bookservice.entity.GenreFactory;
import com.example.bookservice.kafka.ReplyProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;

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

/**
 * Covers the BookServiceImpl paths that talk to Kafka / GenreFactory
 * (the simple repository delegations are covered by {@link BookServiceTest}).
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    private static final String OWNER = "owner@bookbarter.com";

    @Mock
    private BookDAO bookRepository;

    @Mock
    private ReplyProcessor replyProcessor;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private GenreFactory genreFactory;

    @InjectMocks
    private BookServiceImpl bookService;

    private static Book book(long id, BookStatus status) {
        return Book.builder().id(id).title("Book " + id).genres(List.of("novel")).description("d").author("a")
                .year(2000).publishedBy("p").price(3).status(status).userEmail(OWNER).build();
    }

    private static CompletableFuture<String> ownerReply() {
        return CompletableFuture.completedFuture(
                "{\"login\":\"owner\",\"email\":\"" + OWNER + "\",\"rating\":4.0,\"phone\":\"1\",\"bucks\":3}");
    }

    @Test
    void saveBook_mapsDtoToAvailableBookOwnedByCaller() throws Exception {
        BookDTO dto = BookDTO.builder().title("Dune").genres(List.of("novel", "scifi")).description("Spice")
                .author("Herbert").year("1965").publishedBy("Chilton").price("4").build();
        when(genreFactory.translateGenre(List.of("novel", "scifi"))).thenReturn(List.of("Novel", "SciFi"));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book saved = bookService.saveBook(dto, new MockMultipartFile("image", new byte[]{9, 8}), OWNER);

        assertThat(saved.getTitle()).isEqualTo("Dune");
        assertThat(saved.getYear()).isEqualTo(1965);
        assertThat(saved.getPrice()).isEqualTo(4);
        assertThat(saved.getGenres()).containsExactly("Novel", "SciFi");
        assertThat(saved.getImage()).containsExactly(9, 8);
        assertThat(saved.getUserEmail()).isEqualTo(OWNER);
        assertThat(saved.getStatus()).isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    void saveBook_nonNumericPrice_isRejected() {
        BookDTO dto = BookDTO.builder().title("Dune").genres(List.of("novel")).year("1965").price("free").build();

        assertThatThrownBy(() -> bookService.saveBook(dto, new MockMultipartFile("image", new byte[0]), OWNER))
                .isInstanceOf(NumberFormatException.class);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void getAllBooksByEmail_noBooks_returnsEmptyWithoutKafka() {
        when(bookRepository.findByUserEmail(OWNER)).thenReturn(List.of());

        assertThat(bookService.getAllBooksByEmail(OWNER)).isEmpty();
        verifyNoInteractions(kafkaTemplate, replyProcessor);
    }

    @Test
    void getAllBooksByEmail_enrichesBooksWithOwner() {
        when(bookRepository.findByUserEmail(OWNER)).thenReturn(List.of(book(1, BookStatus.AVAILABLE), book(2, BookStatus.SOLD)));
        when(replyProcessor.waitForReply()).thenReturn(ownerReply());
        when(genreFactory.translateGenre(List.of("novel"))).thenReturn(List.of("Novel"));

        List<BookUserDTO> result = bookService.getAllBooksByEmail(OWNER);

        assertThat(result).extracting(BookUserDTO::getId).containsExactly(1L, 2L);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getUser().getEmail()).isEqualTo(OWNER);
            assertThat(dto.getGenres()).containsExactly("Novel");
        });
        // one owner lookup for the whole list
        verify(kafkaTemplate, times(1)).send(any(Message.class));
    }

    @Test
    void getAllAvailable_skipsReservedAndSoldBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(
                book(1, BookStatus.AVAILABLE), book(2, BookStatus.RESERVED), book(3, BookStatus.SOLD), book(4, BookStatus.AVAILABLE)));
        when(replyProcessor.waitForReply()).thenReturn(ownerReply(), ownerReply());
        when(genreFactory.translateGenre(List.of("novel"))).thenReturn(List.of("Novel"));

        List<BookUserDTO> result = bookService.getAllAvailable();

        assertThat(result).extracting(BookUserDTO::getId).containsExactly(1L, 4L);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(2)).send(messages.capture());
        assertThat(messages.getAllValues()).extracting(Message::getPayload).containsOnly(OWNER);
    }

    @Test
    void getAllAvailable_nothingAvailable_returnsEmpty() {
        when(bookRepository.findAll()).thenReturn(List.of(book(2, BookStatus.SOLD)));

        assertThat(bookService.getAllAvailable()).isEmpty();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void getBookById_existingBook_returnsBookWithOwner() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1, BookStatus.AVAILABLE)));
        when(replyProcessor.waitForReply()).thenReturn(ownerReply());
        when(genreFactory.translateGenre(List.of("novel"))).thenReturn(List.of("Novel"));

        BookUserDTO dto = bookService.getBookById(1L);

        assertThat(dto.getTitle()).isEqualTo("Book 1");
        assertThat(dto.getPrice()).isEqualTo(3);
        assertThat(dto.getUser().getLogin()).isEqualTo("owner");
    }

    @Test
    void getBookById_missingBook_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(1L)).hasMessage("Book not found");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void getBookById_malformedOwnerReply_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1, BookStatus.AVAILABLE)));
        when(replyProcessor.waitForReply()).thenReturn(CompletableFuture.completedFuture("not-json"));

        assertThatThrownBy(() -> bookService.getBookById(1L)).hasMessage("Error deserializing user");
    }

    @Test
    void updateBook_missingBook_throws() {
        when(bookRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(book(5, BookStatus.AVAILABLE), 5L)).hasMessage("Book not found");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void getAllBooksByEmailWithoutUser_noBooks_returnsEmpty() {
        when(bookRepository.findByUserEmail(OWNER)).thenReturn(List.of());

        assertThat(bookService.getAllBooksByEmailWithoutUser(OWNER)).isEmpty();
    }
}
