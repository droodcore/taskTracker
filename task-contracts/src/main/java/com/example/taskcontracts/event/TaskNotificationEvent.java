package com.example.taskcontracts.event;

import java.time.LocalDateTime;

public record TaskNotificationEvent(
        TaskEventType eventType,
        Long taskId,
        String title,
        String status,
        String recipient,
        NotificationChannel channel,
        LocalDateTime occurredAt
) {
}
