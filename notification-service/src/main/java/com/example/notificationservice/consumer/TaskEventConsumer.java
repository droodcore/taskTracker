package com.example.notificationservice.consumer;

import com.example.notificationservice.service.NotificationService;
import com.example.taskcontracts.event.TaskEventType;
import com.example.taskcontracts.event.TaskNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topic.task-notifications}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TaskNotificationEvent event) {
        String message = buildMessage(event);
        notificationService.sendNotification(event.channel(), message, event.recipient());
        log.info("Processed task event type={} taskId={} recipient={}",
                event.eventType(), event.taskId(), event.recipient());
    }

    private String buildMessage(TaskNotificationEvent event) {
        return switch (event.eventType()) {
            case CREATED -> "Task '%s' was created with status %s".formatted(event.title(), event.status());
            case UPDATED -> "Task '%s' was updated. Current status: %s".formatted(event.title(), event.status());
            case DELETED -> "Task '%s' was deleted".formatted(event.title());
        };
    }
}
