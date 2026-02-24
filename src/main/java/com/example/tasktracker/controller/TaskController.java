package com.example.tasktracker.controller;

import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Task API", description = "Endpoints for managing tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task", description = "Creates a new task tied to a user and category")
    public ResponseEntity<TaskDto> createTask(@RequestBody TaskDto taskDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskDto));
    }

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Returns a list of all tasks. Can be filtered by category name")
    public ResponseEntity<List<TaskDto>> getAllTasks(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(taskService.getAllTasks(category));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Returns a task by its ID")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task", description = "Updates a task by its ID")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task", description = "Deletes a task by its ID")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
