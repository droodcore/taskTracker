package com.example.tasktracker.controller;

import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@RequestBody TaskDto taskDto) {
        return taskService.createTask(taskDto);
    }

    @GetMapping
    public List<TaskDto> getAllTasks(@RequestParam(required = false) String category) {
        return taskService.getAllTasks(category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }
}
