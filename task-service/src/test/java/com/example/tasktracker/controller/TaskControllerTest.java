package com.example.tasktracker.controller;

import com.example.tasktracker.dto.CreateTaskDto;
import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.exception.ValidationException;
import com.example.tasktracker.model.TaskType;
import com.example.tasktracker.service.CategoryService;
import com.example.tasktracker.service.TaskService;
import com.example.tasktracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserService userService;

    @MockBean
    private CategoryService categoryService;

    @Test
    void createTask() throws Exception {
        TaskDto mockDto = new TaskDto(1L, "Title", "Desc", null, "TODO", TaskType.OTHER, 1L, 1L);
        when(taskService.createTask(any(CreateTaskDto.class))).thenReturn(mockDto);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\", \"userId\":1, \"categoryId\":1, \"status\":\"TODO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void getAllTasks() throws Exception {
        when(taskService.getTasks(null, null, null, null, null, 0, 10, "id", "asc"))
                .thenReturn(new PageImpl<>(List.of(
                        new TaskDto(1L, "Title", "Desc", null, "TODO", TaskType.OTHER, 1L, 1L))));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Title"));
    }

    @Test
    void getAllTasks_WithFiltersPagingAndSorting() throws Exception {
        when(taskService.getTasks("Home", TaskType.WORK, "IN_PROGRESS", null, null, 1, 5, "deadline", "desc"))
                .thenReturn(new PageImpl<>(List.of(
                        new TaskDto(3L, "Filtered", "Desc", null, "IN_PROGRESS", TaskType.WORK, 1L, 2L))));

        mockMvc.perform(get("/tasks")
                        .param("category", "Home")
                        .param("type", "WORK")
                        .param("status", "IN_PROGRESS")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "deadline")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("WORK"));

        verify(taskService).getTasks("Home", TaskType.WORK, "IN_PROGRESS", null, null, 1, 5, "deadline", "desc");
    }

    @Test
    void getAllTasks_WhenValidationFails() throws Exception {
        when(taskService.getTasks(eq(null), eq(null), eq(null), eq(java.time.LocalDate.of(2026, 3, 31)),
                eq(java.time.LocalDate.of(2026, 3, 1)), eq(0), eq(10), eq("id"), eq("asc")))
                .thenThrow(new ValidationException("Invalid request"));

        mockMvc.perform(get("/tasks")
                        .param("deadlineFrom", "2026-03-31")
                        .param("deadlineTo", "2026-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTaskById() throws Exception {
        when(taskService.getTaskById(1L))
                .thenReturn(new TaskDto(1L, "Title", "Desc", null, "TODO", TaskType.OTHER, 1L, 1L));

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void updateTask() throws Exception {
        when(taskService.updateTask(any(Long.class), any(TaskDto.class)))
                .thenReturn(new TaskDto(1L, "Updated title", "Updated desc", null, "IN_PROGRESS", TaskType.WORK, 2L, 3L));

        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"description\":\"Updated desc\",\"status\":\"IN_PROGRESS\",\"userId\":2,\"categoryId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.type").value("WORK"));
    }

    @Test
    void deleteTask() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_WhenNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Task not found with id 42"))
                .when(taskService).deleteTask(42L);

        mockMvc.perform(delete("/tasks/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id 42"));
    }

    @Test
    void getTaskById_WhenNotFound() throws Exception {
        when(taskService.getTaskById(42L)).thenThrow(new ResourceNotFoundException("Task not found with id 42"));

        mockMvc.perform(get("/tasks/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id 42"));
    }
}
