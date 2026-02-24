package com.example.tasktracker.dto;

import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        String title,
        String description,
        LocalDateTime deadline,
        String status,
        Long userId,
        Long categoryId) {
}
