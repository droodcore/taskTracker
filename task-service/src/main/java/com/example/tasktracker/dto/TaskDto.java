package com.example.tasktracker.dto;

import com.example.tasktracker.model.TaskType;
import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        String title,
        String description,
        LocalDateTime deadline,
        String status,
        TaskType type,
        Long userId,
        Long categoryId) {
}
