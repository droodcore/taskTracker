package com.example.tasktracker.controller;

import com.example.tasktracker.dto.CategoryDto;
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

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private UserService userService;

    @MockBean
    private TaskService taskService;

    @Test
    void createCategory() throws Exception {
        CategoryDto mockDto = new CategoryDto(1L, "Work");
        when(categoryService.createCategory(any(CategoryDto.class))).thenReturn(mockDto);

        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Work\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Work"));
    }

    @Test
    void getAllCategories() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(
                new CategoryDto(1L, "Work"),
                new CategoryDto(2L, "Home")));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Work"));
    }

    @Test
    void getCategoryById() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(new CategoryDto(1L, "Work"));

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Work"));
    }

    @Test
    void updateCategory() throws Exception {
        when(categoryService.updateCategory(any(Long.class), any(CategoryDto.class)))
                .thenReturn(new CategoryDto(1L, "Updated"));

        mockMvc.perform(put("/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteCategory() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getCategoryById_WhenNotFound() throws Exception {
        when(categoryService.getCategoryById(99L))
                .thenThrow(new ResourceNotFoundException("Category not found with id 99"));

        mockMvc.perform(get("/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found with id 99"));
    }
}
