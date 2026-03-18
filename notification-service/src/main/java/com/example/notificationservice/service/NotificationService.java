package com.example.notificationservice.service;

import com.example.notificationservice.exception.NotificationDeliveryException;
import com.example.notificationservice.strategy.NotificationSender;
import com.example.taskcontracts.event.NotificationChannel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    public NotificationService(List<NotificationSender> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(NotificationSender::channel, Function.identity()));
    }

    @CircuitBreaker(name = "notification-delivery", fallbackMethod = "fallbackSendNotification")
    public void sendNotification(NotificationChannel notificationChannel, String message, String recipient) {
        NotificationSender notificationSender = senders.get(notificationChannel);
        if (notificationSender == null) {
            throw new IllegalArgumentException("Notification sender not found for channel " + notificationChannel);
        }

        notificationSender.send(message, recipient);
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
