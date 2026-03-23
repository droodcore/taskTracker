package com.example.tasktracker.metrics;

import com.example.tasktracker.model.TaskType;
import com.example.tasktracker.repository.TaskRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskMetricsService {

    private final TaskRepository taskRepository;
    private final MeterRegistry meterRegistry;

    private final Map<String, AtomicLong> statusGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> typeGauges = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (TaskType type : TaskType.values()) {
            AtomicLong value = new AtomicLong(0);
            typeGauges.put(type.name(), value);
            Gauge.builder("tasks_by_type", value, AtomicLong::doubleValue)
                    .tag("type", type.name())
                    .description("Current number of tasks by type")
                    .register(meterRegistry);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void updateMetrics() {
        for (TaskType type : TaskType.values()) {
            long count = taskRepository.findByType(type).size();
            typeGauges.get(type.name()).set(count);
        }

        taskRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        task -> task.getStatus() != null ? task.getStatus() : "UNKNOWN",
                        Collectors.counting()))
                .forEach((status, count) -> {
                    statusGauges.computeIfAbsent(status, s -> {
                        AtomicLong value = new AtomicLong(0);
                        Gauge.builder("tasks_by_status", value, AtomicLong::doubleValue)
                                .tag("status", s)
                                .description("Current number of tasks by status")
                                .register(meterRegistry);
                        return value;
                    }).set(count);
                });
    }
}
