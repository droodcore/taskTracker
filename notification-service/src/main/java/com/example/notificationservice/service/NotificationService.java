package com.example.notificationservice.service;

import com.example.notificationservice.strategy.NotificationSender;
import com.example.taskcontracts.event.NotificationChannel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationService(List<NotificationSender> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(NotificationSender::channel, Function.identity()));
    }

    public void sendNotification(NotificationChannel notificationChannel, String message, String recipient) {
        NotificationSender notificationSender = senders.get(notificationChannel);
        if (notificationSender == null) {
            throw new IllegalArgumentException("Notification sender not found for channel " + notificationChannel);
        }

        notificationSender.send(message, recipient);
    }
}
