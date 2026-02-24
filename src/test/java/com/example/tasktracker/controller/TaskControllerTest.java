package com.example.tasktracker.controller;

import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.service.CategoryService;
import com.example.tasktracker.service.TaskService;
import com.example.tasktracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
        TaskDto mockDto = new TaskDto(1L, "Title", "Desc", null, "TODO", 1L, 1L);
        when(taskService.createTask(any(TaskDto.class))).thenReturn(mockDto);

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Title\", \"userId\":1, \"categoryId\":1, \"status\":\"TODO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void getAllTasks() throws Exception {
        when(taskService.getAllTasks(isNull())).thenReturn(List.of(
                new TaskDto(1L, "Title", "Desc", null, "TODO", 1L, 1L)));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllTasks_WithCategoryFilter() throws Exception {
        when(taskService.getAllTasks("Work")).thenReturn(List.of(
                new TaskDto(2L, "Filtered", "Desc", null, "IN_PROGRESS", 1L, 2L)));

        mockMvc.perform(get("/tasks").param("category", "Work"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Filtered"));

        verify(taskService).getAllTasks("Work");
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
}
