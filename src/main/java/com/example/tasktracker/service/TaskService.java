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

    public TaskDto createTask(TaskDto taskDto) {
        Task task = TaskMapper.INSTANCE.toEntity(taskDto);
        Task savedTask = taskRepository.save(task);
        return TaskMapper.INSTANCE.toDto(savedTask);
    }

    public List<TaskDto> getAllTasks(String category) {
        if (category != null && !category.isEmpty()) {
            return taskRepository.findByCategoryNameIgnoreCase(category).stream()
                    .map(TaskMapper.INSTANCE::toDto)
                    .toList();
        }
        return taskRepository.findAll().stream()
                .map(TaskMapper.INSTANCE::toDto)
                .toList();
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        taskRepository.deleteById(id);
    }
}
