package com.example.notificationservice.service;

import com.example.notificationservice.strategy.NotificationSender;
import com.example.taskcontracts.event.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void sendNotification_UsesMatchingStrategy() {
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);

        NotificationService notificationService = new NotificationService(List.of(emailSender));

        notificationService.sendNotification(NotificationChannel.EMAIL, "hello", "john@example.com");

        verify(emailSender).send("hello", "john@example.com");
    }

    @Test
    void sendNotification_WhenStrategyMissing_ThrowsException() {
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);

        NotificationService notificationService = new NotificationService(List.of(emailSender));

        assertThrows(IllegalArgumentException.class, () ->
                notificationService.sendNotification(NotificationChannel.TELEGRAM, "hello", "@john"));
    }
}
