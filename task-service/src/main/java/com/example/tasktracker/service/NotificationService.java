package com.example.tasktracker.service;

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
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::channel,
                        Function.identity()
                ));
    }

    public void sendNotification(NotificationChannel notificationChannel, String message, String recipient) {
        NotificationSender notificationSender = senders.get(notificationChannel);
        if (notificationSender == null) {
          throw new IllegalArgumentException("Notification sender not found");
        }

        notificationSender.send(message, recipient);
    }

    private interface NotificationSender {
        NotificationChannel channel();
        void send(String message, String recipient);
    }

    private enum NotificationChannel {
        EMAIL,
        TELEGRAM
    }

    private class TelegramNotificationSender implements NotificationSender {

        @Override
        public NotificationChannel channel() {
            return NotificationChannel.TELEGRAM;
        }

        @Override
        public void send(String message, String recipient) {
            log.info("Sending message to telegram {}: {}", recipient, message);
        }
    }
}
