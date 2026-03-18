package com.example.notificationservice.strategy;

import com.example.taskcontracts.event.NotificationChannel;

public interface NotificationSender {

    NotificationChannel channel();

    void send(String message, String recipient);
}
