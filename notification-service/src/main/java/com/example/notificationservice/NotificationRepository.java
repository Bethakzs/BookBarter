package com.example.notificationservice;

import com.example.notificationservice.dto.responseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<responseDTO> findAllByEmail(String email);
}
