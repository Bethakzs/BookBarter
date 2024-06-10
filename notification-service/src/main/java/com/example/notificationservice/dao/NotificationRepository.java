package com.example.notificationservice.dao;

import com.example.notificationservice.dto.response.NotificationDTO;
import com.example.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<NotificationDTO> findAllByEmail(String email);
}
