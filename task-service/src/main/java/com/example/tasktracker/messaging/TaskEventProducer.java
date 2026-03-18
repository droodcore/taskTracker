package com.example.tasktracker.messaging;

import com.example.tasktracker.model.Task;
import com.example.taskcontracts.event.NotificationChannel;
import com.example.taskcontracts.event.TaskEventType;
import com.example.taskcontracts.event.TaskNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventProducer {

    private final KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate;

    @Value("${app.kafka.topic.task-notifications}")
    private String taskNotificationsTopic;

    public void sendTaskEvent(TaskEventType eventType, Task task) {
        TaskNotificationEvent event = new TaskNotificationEvent(
                eventType,
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getUser().getEmail(),
                NotificationChannel.EMAIL,
                LocalDateTime.now());

        Runnable publishAction = () -> kafkaTemplate.send(taskNotificationsTopic, String.valueOf(task.getId()), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish task event type={} taskId={}", eventType, task.getId(), throwable);
                        return;
                    }

                    log.info("Published task event type={} taskId={} topic={}",
                            eventType, task.getId(), taskNotificationsTopic);
                });

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }
}
