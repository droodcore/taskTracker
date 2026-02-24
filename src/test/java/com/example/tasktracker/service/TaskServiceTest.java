package com.example.tasktracker.service;

import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.model.Category;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.User;
import com.example.tasktracker.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

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
