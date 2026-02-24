package com.example.tasktracker.service;

import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskDto createTask(TaskDto taskDto) {
        Task task = taskMapper.toEntity(taskDto);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toDto(savedTask);
    }

    public List<TaskDto> getAllTasks(String category) {
        if (category != null && !category.isEmpty()) {
            return taskRepository.findByCategoryNameIgnoreCase(category).stream()
                    .map(taskMapper::toDto)
                    .toList();
        }
        return taskRepository.findAll().stream()
                .map(taskMapper::toDto)
                .toList();
    }

    public TaskDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
        return taskMapper.toDto(task);
    }

    public TaskDto updateTask(Long id, TaskDto taskDto) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        existingTask.setTitle(taskDto.title());
        existingTask.setDescription(taskDto.description());
        existingTask.setDeadline(taskDto.deadline());
        existingTask.setStatus(taskDto.status());

        if (taskDto.userId() != null) {
            existingTask.setUser(taskMapper.mapUser(taskDto.userId()));
        }
        if (taskDto.categoryId() != null) {
            existingTask.setCategory(taskMapper.mapCategory(taskDto.categoryId()));
        }

        Task updatedTask = taskRepository.save(existingTask);
        return taskMapper.toDto(updatedTask);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        taskRepository.deleteById(id);
    }
}
