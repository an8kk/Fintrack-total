package com.fintrack.backend.service;

import com.fintrack.backend.entity.Notification;
import com.fintrack.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getByUserId(Long userId) {
        log.debug("Fetching notifications for userId={}", userId);
        List<Notification> notifs = notificationRepository.findByUserIdOrderByDateDesc(userId);
        log.debug("Found {} notifications for userId={}", notifs.size(), userId);
        return notifs;
    }
}
