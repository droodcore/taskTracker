package com.example.tasktracker.service;

import com.example.tasktracker.dto.CreateTaskDto;
import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskType;
import com.example.tasktracker.repository.CategoryRepository;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TaskMapper taskMapper;

    @CacheEvict(value = "taskLists", allEntries = true)
    public TaskDto createTask(CreateTaskDto request) {
        Task task = taskMapper.toEntity(request);
        task.setType(request.type() != null ? request.type() : TaskType.OTHER);
        task.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.userId())));
        task.setCategory(categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.categoryId())));
        TaskDto created = taskMapper.toDto(taskRepository.save(task));
        log.info("Created task id={} title='{}' type={}", created.id(), created.title(), created.type());
        return created;
    }

    public List<TaskDto> getAllTasks(String category) {
        return getAllTasks(category, null);
    }

    @Cacheable(value = "taskLists")
    public List<TaskDto> getAllTasks(String category, TaskType type) {
        if (category != null && !category.isEmpty() && type != null) {
            return taskRepository.findByCategoryNameIgnoreCaseAndType(category, type).stream()
                    .map(taskMapper::toDto)
                    .toList();
        }
        if (category != null && !category.isEmpty()) {
            return taskRepository.findByCategoryNameIgnoreCase(category).stream()
                    .map(taskMapper::toDto)
                    .toList();
        }
        if (type != null) {
            return taskRepository.findByType(type).stream()
                    .map(taskMapper::toDto)
                    .toList();
        }
        return taskRepository.findAll().stream()
                .map(taskMapper::toDto)
                .toList();
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

        TaskDto updated = taskMapper.toDto(taskRepository.save(existingTask));
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
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        taskRepository.deleteById(id);
        log.info("Deleted task id={}", id);
    }
}
