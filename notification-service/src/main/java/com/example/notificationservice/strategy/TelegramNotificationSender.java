package com.example.notificationservice.strategy;

import com.example.taskcontracts.event.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TelegramNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.TELEGRAM;
    }

    @Override
    public void send(String message, String recipient) {
        log.info("Sending telegram message to {}: {}", recipient, message);
    }
}
