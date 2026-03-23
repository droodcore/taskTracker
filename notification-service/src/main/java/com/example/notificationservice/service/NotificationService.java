package com.example.notificationservice.service;

import com.example.notificationservice.exception.NotificationDeliveryException;
import com.example.notificationservice.strategy.NotificationSender;
import com.example.taskcontracts.event.NotificationChannel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private final Map<NotificationChannel, NotificationSender> senders;
    private final MeterRegistry meterRegistry;

    public NotificationService(List<NotificationSender> senders, MeterRegistry meterRegistry) {
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(NotificationSender::channel, Function.identity()));
        this.meterRegistry = meterRegistry;
    }

    @CircuitBreaker(name = "notification-delivery", fallbackMethod = "fallbackSendNotification")
    public void sendNotification(NotificationChannel notificationChannel, String message, String recipient) {
        NotificationSender notificationSender = senders.get(notificationChannel);
        if (notificationSender == null) {
            throw new IllegalArgumentException("Notification sender not found for channel " + notificationChannel);
        }

        notificationSender.send(message, recipient);
        meterRegistry.counter("notifications_sent_total", "channel", notificationChannel.name()).increment();
    }

    @SuppressWarnings("unused")
    void fallbackSendNotification(
            NotificationChannel notificationChannel,
            String message,
            String recipient,
            Throwable throwable) {
        log.warn("Notification delivery fallback triggered channel={} recipient={}",
                notificationChannel, recipient, throwable);
        throw new NotificationDeliveryException("Notification delivery failed", throwable);
    }
}
