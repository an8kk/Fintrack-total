package com.fintrack.backend.service;

import com.fintrack.backend.entity.Notification;
import com.fintrack.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByDateDesc(userId);
    }
}
