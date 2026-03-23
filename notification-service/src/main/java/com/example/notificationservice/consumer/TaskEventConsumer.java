package com.example.notificationservice.consumer;

import com.example.notificationservice.service.NotificationService;
import com.example.taskcontracts.event.TaskNotificationEvent;
import io.micrometer.core.instrument.MeterRegistry;
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
public class TaskEventConsumer {

    private static final Logger dltLogger = LoggerFactory.getLogger("notification-dlt");

    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;

    public TaskEventConsumer(NotificationService notificationService, MeterRegistry meterRegistry) {
        this.notificationService = notificationService;
        this.meterRegistry = meterRegistry;
    }

    @RetryableTopic(
            attempts = "${app.kafka.retry.attempts}",
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
    public void consume(
            TaskNotificationEvent event,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.DELIVERY_ATTEMPT, required = false) Integer deliveryAttempt) {
        if (deliveryAttempt != null && deliveryAttempt > 1) {
            log.warn(
                    "Retrying task event attempt={} topic={} taskId={} eventType={} recipient={}",
                    deliveryAttempt, topic, event.taskId(), event.eventType(), event.recipient());
        }

        String message = event.eventType().buildMessage(event);
        notificationService.sendNotification(event.channel(), message, event.recipient());
        log.info("Processed task event type={} taskId={} recipient={}",
                event.eventType(), event.taskId(), event.recipient());
        meterRegistry.counter("kafka_events_consumed_total", "event_type", event.eventType().name()).increment();
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
        meterRegistry.counter("dlt_messages_total").increment();
    }
}
