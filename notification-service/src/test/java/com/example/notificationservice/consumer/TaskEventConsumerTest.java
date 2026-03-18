package com.example.notificationservice.consumer;

import com.example.notificationservice.service.NotificationService;
import com.example.taskcontracts.event.NotificationChannel;
import com.example.taskcontracts.event.TaskEventType;
import com.example.taskcontracts.event.TaskNotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class TaskEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskEventConsumer taskEventConsumer;

    @Test
    void consume_SendsNotificationUsingEventData() {
        TaskNotificationEvent event = new TaskNotificationEvent(
                TaskEventType.CREATED,
                1L,
                "Prepare report",
                "TODO",
                "john@example.com",
                NotificationChannel.EMAIL,
                LocalDateTime.now());

        taskEventConsumer.consume(event, "task-notifications", 1);

        verify(notificationService).sendNotification(
                NotificationChannel.EMAIL,
                "Task 'Prepare report' was created with status TODO",
                "john@example.com");
    }

    @Test
    void handleDlt_LogsDeadLetterEventWithoutThrowing() {
        TaskNotificationEvent event = new TaskNotificationEvent(
                TaskEventType.DELETED,
                7L,
                "Archive task",
                "DONE",
                "john@example.com",
                NotificationChannel.EMAIL,
                LocalDateTime.now());

        assertDoesNotThrow(() -> taskEventConsumer.handleDlt(event, "task-notifications-dlt", 0, 42L));
    }
}
