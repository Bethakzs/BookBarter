package com.example.notificationservice.kafka;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.service.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    @Test
    void handleUpdateBookRequest_parsesPayloadIntoNotification() {
        kafkaConsumer.handleUpdateBookRequest("Dune:seller@bookbarter.com:buyer:+380000000000:4.5:7");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).add(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.getTitle()).isEqualTo("Dune");
        assertThat(notification.getEmail()).isEqualTo("seller@bookbarter.com");
        assertThat(notification.getLogin()).isEqualTo("buyer");
        assertThat(notification.getPhone()).isEqualTo("+380000000000");
        assertThat(notification.getRating()).isEqualTo(4.5);
        assertThat(notification.getPrice()).isEqualTo(7);
    }

    @Test
    void handleUpdateBookRequest_truncatedPayload_isRejected() {
        assertThatThrownBy(() -> kafkaConsumer.handleUpdateBookRequest("Dune:seller@bookbarter.com"))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(notificationService);
    }
}
