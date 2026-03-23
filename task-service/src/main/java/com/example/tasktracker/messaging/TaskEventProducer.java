package com.example.tasktracker.messaging;

import com.example.tasktracker.model.Task;
import com.example.taskcontracts.event.NotificationChannel;
import com.example.taskcontracts.event.TaskEventType;
import com.example.taskcontracts.event.TaskNotificationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Component
public class TaskEventProducer {

    private final KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${app.kafka.topic.task-notifications}")
    private String taskNotificationsTopic;

    public TaskEventProducer(
            KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    public void sendTaskEvent(TaskEventType eventType, Task task) {
        TaskNotificationEvent event = new TaskNotificationEvent(
                eventType,
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getUser().getEmail(),
                NotificationChannel.EMAIL,
                LocalDateTime.now());

        String traceId = MDC.get("trace_id");

        Runnable publishAction = () -> {
            Message<TaskNotificationEvent> message =
                    MessageBuilder
                            .withPayload(event)
                            .setHeader(KafkaHeaders.TOPIC, taskNotificationsTopic)
                            .setHeader(KafkaHeaders.KEY, String.valueOf(task.getId()))
                            .setHeader("trace_id", traceId != null ? traceId : "")
                            .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            meterRegistry.counter("kafka_publish_failures_total", "event_type", eventType.name()).increment();
                            log.error("Failed to publish task event type={} taskId={}", eventType, task.getId(), throwable);
                            return;
                        }

                        meterRegistry.counter("kafka_events_published_total", "event_type", eventType.name()).increment();
                        log.info("Published task event type={} taskId={} topic={}",
                                eventType, task.getId(), taskNotificationsTopic);
                    });
        };

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
