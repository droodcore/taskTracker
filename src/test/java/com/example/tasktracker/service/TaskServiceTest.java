package com.example.tasktracker.service;

import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.model.Category;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.User;
import com.example.tasktracker.repository.CategoryRepository;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private Task task;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).build();
        Category category = Category.builder().id(1L).name("Work").build();

        task = Task.builder()
                .id(1L)
                .title("Do work")
                .status("TODO")
                .user(user)
                .category(category)
                .build();
    }

    @Test
    void createTask() {
        TaskDto dto = new TaskDto(null, "Do work", null, null, "TODO", 1L, 1L);
        when(taskMapper.toEntity(dto)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toDto(task)).thenReturn(new TaskDto(1L, "Do work", null, null, "TODO", 1L, 1L));

        TaskDto result = taskService.createTask(dto);

        assertNotNull(result);
        assertEquals("Do work", result.title());
        assertEquals(1L, result.userId());
        assertEquals(1L, result.categoryId());
        verify(taskMapper).toEntity(dto);
        verify(taskMapper).toDto(task);
    }

    @Test
    void getAllTasks_NoCategory() {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(taskMapper.toDto(task)).thenReturn(new TaskDto(1L, "Do work", null, null, "TODO", 1L, 1L));

        List<TaskDto> results = taskService.getAllTasks(null);

        assertEquals(1, results.size());
        assertEquals("Do work", results.get(0).title());
        verify(taskMapper).toDto(task);
    }

    @Test
    void getAllTasks_WithCategory() {
        when(taskRepository.findByCategoryNameIgnoreCase("Work")).thenReturn(List.of(task));
        when(taskMapper.toDto(task)).thenReturn(new TaskDto(1L, "Do work", null, null, "TODO", 1L, 1L));

        List<TaskDto> results = taskService.getAllTasks("Work");

        assertEquals(1, results.size());
        assertEquals("Do work", results.get(0).title());
        verify(taskMapper).toDto(task);
    }

    @Test
    void getTaskById() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toDto(task)).thenReturn(new TaskDto(1L, "Do work", null, null, "TODO", 1L, 1L));

        TaskDto result = taskService.getTaskById(1L);

        assertEquals(1L, result.id());
        assertEquals("Do work", result.title());
    }

    @Test
    void getTaskById_WhenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(99L));
    }

    @Test
    void updateTask() {
        TaskDto dto = new TaskDto(null, "Updated title", "Updated description", null, "IN_PROGRESS", 2L, 3L);
        User updatedUser = User.builder().id(2L).build();
        Category updatedCategory = Category.builder().id(3L).name("Home").build();
        Task updatedTask = Task.builder()
                .id(1L)
                .title("Updated title")
                .description("Updated description")
                .status("IN_PROGRESS")
                .user(updatedUser)
                .category(updatedCategory)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(updatedUser));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(updatedCategory));
        when(taskRepository.save(task)).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask))
                .thenReturn(new TaskDto(1L, "Updated title", "Updated description", null, "IN_PROGRESS", 2L, 3L));

        TaskDto result = taskService.updateTask(1L, dto);

        assertEquals("Updated title", result.title());
        assertEquals("IN_PROGRESS", result.status());
        assertEquals(2L, result.userId());
        assertEquals(3L, result.categoryId());
    }

    @Test
    void updateTask_WhenNotFound() {
        TaskDto dto = new TaskDto(null, "Updated", null, null, "DONE", 1L, 1L);
        when(taskRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(100L, dto));
    }

    @Test
    void updateTask_WhenUserNotFound() {
        TaskDto dto = new TaskDto(null, "Updated title", "Updated description", null, "IN_PROGRESS", 99L, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(1L, dto));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_WhenCategoryNotFound() {
        TaskDto dto = new TaskDto(null, "Updated title", "Updated description", null, "IN_PROGRESS", null, 99L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(1L, dto));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_WhenExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(1L);

        assertDoesNotThrow(() -> taskService.deleteTask(1L));
        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTask_WhenNotExists() {
        when(taskRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(1L));
        verify(taskRepository, never()).deleteById(1L);
    }
}
