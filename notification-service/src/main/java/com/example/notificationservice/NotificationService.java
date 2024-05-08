package com.example.notificationservice;

import com.example.notificationservice.dto.responseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<responseDTO> getAll(String email) {
        return notificationRepository.findAllByEmail(email);
    }

    public void add(Notification notification) {
        notificationRepository.save(notification);
    }
}
