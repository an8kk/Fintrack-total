package com.fintrack.backend.controller;

import com.fintrack.backend.entity.Notification;
import com.fintrack.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/data/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long userId) {
        log.info("GET /api/data/notifications/{}", userId);
        List<Notification> notifs = notificationService.getByUserId(userId);
        log.debug("Returning {} notifications for userId={}", notifs.size(), userId);
        return ResponseEntity.ok(notifs);
    }
}
