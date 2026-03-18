package com.example.tasktracker.service;

import com.example.tasktracker.dto.CreateTaskDto;
import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.exception.ValidationException;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.messaging.TaskEventProducer;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskType;
import com.example.tasktracker.repository.CategoryRepository;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.repository.UserRepository;
import com.example.taskcontracts.event.TaskEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TaskMapper taskMapper;
    private final TaskEventProducer taskEventProducer;

    @Transactional
    @CacheEvict(value = "taskLists", allEntries = true)
    public TaskDto createTask(CreateTaskDto request) {
        Task task = taskMapper.toEntity(request);
        task.setType(request.type() != null ? request.type() : TaskType.OTHER);
        task.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.userId())));
        task.setCategory(categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.categoryId())));

        Task savedTask = taskRepository.save(task);
        taskEventProducer.sendTaskEvent(TaskEventType.CREATED, savedTask);

        TaskDto created = taskMapper.toDto(savedTask);
        log.info("Created task id={} title='{}' type={}", created.id(), created.title(), created.type());
        return created;
    }

    @Cacheable(value = "taskLists")
    public Page<TaskDto> getTasks(
            String category,
            TaskType type,
            String status,
            LocalDate deadlineFrom,
            LocalDate deadlineTo,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        if (deadlineFrom != null && deadlineTo != null && deadlineFrom.isAfter(deadlineTo)) {
            throw new ValidationException("deadlineFrom must be before or equal to deadlineTo");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(resolveDirection(sortDirection), sortBy));
        LocalDateTime startDateTime = deadlineFrom != null ? deadlineFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = deadlineTo != null ? deadlineTo.plusDays(1).atStartOfDay().minusNanos(1) : null;

        return taskRepository.searchTasks(category, type, status, startDateTime, endDateTime, pageable)
                .map(taskMapper::toDto);
    }

    @Cacheable(value = "tasks", key = "#id")
    public TaskDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
        return taskMapper.toDto(task);
    }

    @Caching(
            put = @CachePut(value = "tasks", key = "#id"),
            evict = @CacheEvict(value = "taskLists", allEntries = true))
    @Transactional
    public TaskDto updateTask(Long id, TaskDto taskDto) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        existingTask.setTitle(taskDto.title());
        existingTask.setDescription(taskDto.description());
        existingTask.setDeadline(taskDto.deadline());
        existingTask.setStatus(taskDto.status());
        if (taskDto.type() != null) {
            existingTask.setType(taskDto.type());
        }

        if (taskDto.userId() != null) {
            existingTask.setUser(userRepository.findById(taskDto.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + taskDto.userId())));
        }
        if (taskDto.categoryId() != null) {
            existingTask.setCategory(categoryRepository.findById(taskDto.categoryId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Category not found with id " + taskDto.categoryId())));
        }

        Task savedTask = taskRepository.save(existingTask);
        taskEventProducer.sendTaskEvent(TaskEventType.UPDATED, savedTask);

        TaskDto updated = taskMapper.toDto(savedTask);
        log.info("Updated task id={}", id);
        return updated;
    }

    public List<TaskDto> getTasksByType(TaskType type) {
        return taskRepository.findByType(type).stream()
                .map(taskMapper::toDto)
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#id"),
            @CacheEvict(value = "taskLists", allEntries = true)})
    @Transactional
    public void deleteTask(Long id) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        taskRepository.delete(existingTask);
        taskEventProducer.sendTaskEvent(TaskEventType.DELETED, existingTask);
        log.info("Deleted task id={}", id);
    }

    private Sort.Direction resolveDirection(String sortDirection) {
        return "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }
}
