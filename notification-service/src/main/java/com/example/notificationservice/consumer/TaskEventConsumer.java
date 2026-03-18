package com.example.notificationservice.consumer;

import com.example.notificationservice.service.NotificationService;
import com.example.taskcontracts.event.TaskNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Backoff;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventConsumer {

    private static final Logger dltLogger = LoggerFactory.getLogger("notification-dlt");

    private final NotificationService notificationService;

    @RetryableTopic(
            attempts = "${app.kafka.retry.attempts}",
            kafkaTemplate = "retryTopicKafkaTemplate",
            dltTopicSuffix = "${app.kafka.topic.dlt-suffix}",
            autoCreateTopics = "${app.kafka.retry.auto-create-topics:true}",
            numPartitions = "${app.kafka.topic.partitions:3}",
            replicationFactor = "${app.kafka.topic.replication-factor:3}",
            backoff = @Backoff(
                    delayExpression = "${app.kafka.retry.initial-delay-ms}",
                    multiplierExpression = "${app.kafka.retry.multiplier}",
                    maxDelayExpression = "${app.kafka.retry.max-delay-ms}"
            )
    )
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

    @DltHandler
    public void handleDlt(
            TaskNotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        dltLogger.error(
                "Event routed to DLT topic={} partition={} offset={} taskId={} eventType={} recipient={}",
                topic, partition, offset, event.taskId(), event.eventType(), event.recipient());
    }
}
