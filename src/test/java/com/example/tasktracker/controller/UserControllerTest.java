package com.example.tasktracker.controller;

import com.example.tasktracker.dto.CreateUserDto;
import com.example.tasktracker.dto.UserDto;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private TaskService taskService;

    @Test
    void createUser() throws Exception {
        UserDto mockDto = new UserDto(1L, "john", "john@example.com");
        when(userService.createUser(any(CreateUserDto.class))).thenReturn(mockDto);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"john\", \"email\":\"john@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void getAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                new UserDto(1L, "john", "john@example.com")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(new UserDto(1L, "john", "john@example.com"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void updateUser() throws Exception {
        when(userService.updateUser(any(Long.class), any(CreateUserDto.class)))
                .thenReturn(new UserDto(1L, "john-updated", "john-updated@example.com"));

        mockMvc.perform(put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"john-updated\", \"email\":\"john-updated@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john-updated"));
    }

    @Test
    void deleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUserById_WhenNotFound() throws Exception {
        when(userService.getUserById(42L)).thenThrow(new ResourceNotFoundException("User not found with id 42"));

        mockMvc.perform(get("/users/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id 42"));
    }
}
