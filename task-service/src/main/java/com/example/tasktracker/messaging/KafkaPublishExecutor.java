package com.example.tasktracker.messaging;

import com.example.taskcontracts.event.TaskEventType;
import com.example.taskcontracts.event.TaskNotificationEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class KafkaPublishExecutor {

    private final KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final Counter fallbackCounter;

    public KafkaPublishExecutor(
            KafkaTemplate<String, TaskNotificationEvent> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.fallbackCounter = Counter.builder("kafka_publish_fallback_total")
                .description("Total Kafka publish circuit breaker fallbacks")
                .register(meterRegistry);
    }

    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "fallbackSend")
    @Retry(name = "kafkaPublisher")
    public void send(String topic, TaskNotificationEvent event, Long taskId,
                     TaskEventType eventType, String traceId) {
        Message<TaskNotificationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, String.valueOf(taskId))
                .setHeader("trace_id", traceId != null ? traceId : "")
                .build();

        try {
            kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);
            meterRegistry.counter("kafka_events_published_total", "event_type", eventType.name()).increment();
            log.info("Published task event type={} taskId={} topic={}", eventType, taskId, topic);
        } catch (ExecutionException e) {
            meterRegistry.counter("kafka_publish_failures_total", "event_type", eventType.name()).increment();
            log.error("Failed to publish task event type={} taskId={}", eventType, taskId, e.getCause());
            throw new KafkaException("Kafka send failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            meterRegistry.counter("kafka_publish_failures_total", "event_type", eventType.name()).increment();
            throw new KafkaException("Kafka send interrupted", e);
        } catch (TimeoutException e) {
            meterRegistry.counter("kafka_publish_failures_total", "event_type", eventType.name()).increment();
            log.error("Kafka send timed out type={} taskId={}", eventType, taskId);
            throw new KafkaException("Kafka send timed out", e);
        }
    }

    @SuppressWarnings("unused")
    public void fallbackSend(String topic, TaskNotificationEvent event, Long taskId,
                              TaskEventType eventType, String traceId, Throwable throwable) {
        fallbackCounter.increment();
        log.warn("Circuit breaker fallback for task event type={} taskId={}: {}",
                eventType, taskId, throwable.getMessage());
    }
}
