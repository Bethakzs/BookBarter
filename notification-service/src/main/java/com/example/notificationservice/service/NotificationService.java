package com.example.notificationservice.service;

import com.example.notificationservice.dto.response.NotificationDTO;
import com.example.notificationservice.entity.Notification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NotificationService {
    List<NotificationDTO> getAll(String email);

    void add(Notification notification);
}
