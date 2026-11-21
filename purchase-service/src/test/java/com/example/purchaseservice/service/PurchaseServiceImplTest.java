package com.example.purchaseservice.service;

import com.example.purchaseservice.dao.PurchaseRepository;
import com.example.purchaseservice.dto.secondly.BookDTO;
import com.example.purchaseservice.dto.secondly.BookStatus;
import com.example.purchaseservice.dto.secondly.UserDTO;
import com.example.purchaseservice.entity.Purchase;
import com.example.purchaseservice.entity.PurchaseStatus;
import com.example.purchaseservice.kafka.ReplyProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
class PurchaseServiceImplTest {

    private static final String BUYER = "buyer@bookbarter.com";
    private static final String SELLER = "seller@bookbarter.com";
    private static final long BOOK_ID = 5L;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ReplyProcessor replyProcessor;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private String userJson(String email, long bucks) throws Exception {
        return mapper.writeValueAsString(UserDTO.builder()
                .login("user-" + email).email(email).phone("+380000000000").rating(4.0).bucks(bucks).build());
    }

    private String bookJson(int price) throws Exception {
        return mapper.writeValueAsString(BookDTO.builder()
                .id(BOOK_ID).title("Dune").price(price).status(BookStatus.AVAILABLE).userEmail(SELLER).build());
    }

    private static Purchase purchase(PurchaseStatus status) {
        return Purchase.builder().id(1L).buyerEmail(BUYER).sellerEmail(SELLER).bookId(BOOK_ID).status(status).build();
    }

    /** Captures every Kafka message sent and returns topic -> payload (in send order). */
    @SuppressWarnings("rawtypes")
    private Map<String, Object> sentMessages(int expectedCount) {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(expectedCount)).send(captor.capture());
        Map<String, Object> byTopic = new LinkedHashMap<>();
        List<Message> all = captor.getAllValues();
        for (Message message : all) {
            byTopic.put((String) message.getHeaders().get(KafkaHeaders.TOPIC), message.getPayload());
        }
        return byTopic;
    }

    // ---------- buyBook ----------

    @Test
    void buyBook_enoughBucks_reservesBookChargesBuyerAndNotifies() throws Exception {
        when(replyProcessor.waitForReply()).thenReturn(
                CompletableFuture.completedFuture(userJson(BUYER, 10)),
                CompletableFuture.completedFuture(bookJson(4)));
        Purchase purchase = Purchase.builder().buyerEmail(BUYER).sellerEmail(SELLER).bookId(BOOK_ID).build();

        Purchase result = purchaseService.buyBook(purchase);

        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.RESERVED);
        verify(purchaseRepository).save(purchase);

        Map<String, Object> sent = sentMessages(8);
        assertThat(sent.get("user-service-request-get-user-by-email-topic")).isEqualTo(BUYER);
        assertThat(sent.get("book-service-request-get-book-by-id-topic")).isEqualTo("5");
        assertThat(sent.get("book-service-request-update-book-topic")).isEqualTo("5:RESERVED");
        assertThat(sent.get("user-service-request-update-user-topic")).isEqualTo(BUYER + ":6");
        assertThat(sent.get("email-service-request-buy-book")).isEqualTo(BUYER + ":Dune");
        assertThat(sent.get("email-service-request-sell-book")).isEqualTo(SELLER + ":+380000000000:" + BUYER);
        assertThat(sent.get("notification-service-request-add-request"))
                .isEqualTo("Dune:" + SELLER + ":user-" + BUYER + ":+380000000000:4.0:4");
        assertThat(sent.get("user-service-request-change-notification-true")).isEqualTo(SELLER);
    }

    @Test
    void buyBook_notEnoughBucks_throwsAndDoesNotSave() throws Exception {
        when(replyProcessor.waitForReply()).thenReturn(
                CompletableFuture.completedFuture(userJson(BUYER, 2)),
                CompletableFuture.completedFuture(bookJson(4)));
        Purchase purchase = Purchase.builder().buyerEmail(BUYER).sellerEmail(SELLER).bookId(BOOK_ID).build();

        assertThatThrownBy(() -> purchaseService.buyBook(purchase))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not enough money");

        verify(purchaseRepository, never()).save(any());
        assertThat(purchase.getStatus()).isNull();
        // only the two lookup requests were sent
        sentMessages(2);
    }

    @Test
    void buyBook_malformedUserReply_throws() {
        when(replyProcessor.waitForReply()).thenReturn(CompletableFuture.completedFuture("not-json"));
        Purchase purchase = Purchase.builder().buyerEmail(BUYER).sellerEmail(SELLER).bookId(BOOK_ID).build();

        assertThatThrownBy(() -> purchaseService.buyBook(purchase))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error deserializing user");
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void buyBook_malformedBookReply_throws() throws Exception {
        when(replyProcessor.waitForReply()).thenReturn(
                CompletableFuture.completedFuture(userJson(BUYER, 10)),
                CompletableFuture.completedFuture("not-json"));
        Purchase purchase = Purchase.builder().buyerEmail(BUYER).sellerEmail(SELLER).bookId(BOOK_ID).build();

        assertThatThrownBy(() -> purchaseService.buyBook(purchase))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error deserializing book");
        verifyNoInteractions(purchaseRepository);
    }

    // ---------- confirmPurchase ----------

    @Test
    void confirmPurchase_unknownId_throws() {
        when(purchaseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.confirmPurchase(1L, BUYER)).hasMessage("Purchase not found");
        verifyNoInteractions(kafkaTemplate, replyProcessor);
    }

    @Test
    void confirmPurchase_alreadyConfirmed_throws() {
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase(PurchaseStatus.CONFIRMED)));

        assertThatThrownBy(() -> purchaseService.confirmPurchase(1L, BUYER)).hasMessage("Purchase is not reserved");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void confirmPurchase_notTheBuyer_throws() {
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase(PurchaseStatus.RESERVED)));

        assertThatThrownBy(() -> purchaseService.confirmPurchase(1L, "intruder@bookbarter.com"))
                .hasMessage("You are not the buyer");
        verifyNoInteractions(kafkaTemplate);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void confirmPurchase_reservedByBuyer_marksSoldPaysSellerAndConfirms() throws Exception {
        Purchase purchase = purchase(PurchaseStatus.RESERVED);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(replyProcessor.waitForReply()).thenReturn(
                CompletableFuture.completedFuture(userJson(SELLER, 20)),
                CompletableFuture.completedFuture(bookJson(4)));

        purchaseService.confirmPurchase(1L, BUYER);

        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.CONFIRMED);
        verify(purchaseRepository).save(purchase);

        Map<String, Object> sent = sentMessages(4);
        assertThat(sent.get("user-service-request-get-user-by-email-topic")).isEqualTo(SELLER);
        assertThat(sent.get("book-service-request-update-book-topic")).isEqualTo("5:SOLD");
        assertThat(sent.get("user-service-request-update-user-topic")).isEqualTo(SELLER + ":24");
    }
}
