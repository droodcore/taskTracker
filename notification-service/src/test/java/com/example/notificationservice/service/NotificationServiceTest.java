package com.example.notificationservice.service;

import com.example.notificationservice.strategy.NotificationSender;
import com.example.taskcontracts.event.NotificationChannel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final MeterRegistry meterRegistry = mock(MeterRegistry.class);
    private final Counter counter = mock(Counter.class);

    @Test
    void sendNotification_UsesMatchingStrategy() {
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);

        NotificationService notificationService = new NotificationService(List.of(emailSender), meterRegistry);

        notificationService.sendNotification(NotificationChannel.EMAIL, "hello", "john@example.com");

        verify(emailSender).send("hello", "john@example.com");
    }

    @Test
    void sendNotification_WhenStrategyMissing_ThrowsException() {
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);

        NotificationService notificationService = new NotificationService(List.of(emailSender), meterRegistry);

        assertThrows(IllegalArgumentException.class, () ->
                notificationService.sendNotification(NotificationChannel.TELEGRAM, "hello", "@john"));
    }
}
