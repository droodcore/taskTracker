package com.example.notificationservice.strategy;

import com.example.taskcontracts.event.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(String message, String recipient) {
        log.info("Sending email to {}: {}", recipient, message);
    }
}
