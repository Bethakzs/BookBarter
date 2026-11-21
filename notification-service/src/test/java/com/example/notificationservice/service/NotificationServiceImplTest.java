package com.example.notificationservice.service;

import com.example.notificationservice.dao.NotificationRepository;
import com.example.notificationservice.dto.response.NotificationDTO;
import com.example.notificationservice.entity.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final String EMAIL = "seller@bookbarter.com";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @SuppressWarnings("rawtypes")
    private Message sentMessage() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void getAll_returnsNotificationsAndResetsUserNotificationFlag() {
        List<NotificationDTO> stored = List.of(
                NotificationDTO.builder().id(1L).title("Dune").login("buyer").phone("1").rating(4.0).price(4).build(),
                NotificationDTO.builder().id(2L).title("Emma").login("other").phone("2").rating(3.5).price(2).build());
        when(notificationRepository.findAllByEmail(EMAIL)).thenReturn(stored);

        List<NotificationDTO> result = notificationService.getAll(EMAIL);

        assertThat(result).isEqualTo(stored);
        Message<?> message = sentMessage();
        assertThat(message.getPayload()).isEqualTo(EMAIL);
        assertThat(message.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("user-service-request-change-notification-false");
        assertThat(message.getHeaders().get("serviceName")).isEqualTo("user-service");
    }

    @Test
    void getAll_noNotifications_stillResetsFlag() {
        when(notificationRepository.findAllByEmail(EMAIL)).thenReturn(List.of());

        assertThat(notificationService.getAll(EMAIL)).isEmpty();
        assertThat(sentMessage().getPayload()).isEqualTo(EMAIL);
    }

    @Test
    void add_persistsNotificationWithoutKafka() {
        Notification notification = Notification.builder()
                .email(EMAIL).title("Dune").login("buyer").phone("1").rating(4.0).price(4).build();

        notificationService.add(notification);

        verify(notificationRepository).save(notification);
        verifyNoInteractions(kafkaTemplate);
    }
}
