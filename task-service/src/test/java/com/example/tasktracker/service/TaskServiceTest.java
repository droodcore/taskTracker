package com.example.tasktracker.service;

import com.example.tasktracker.dto.CreateTaskDto;
import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.exception.ValidationException;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.messaging.TaskEventProducer;
import com.example.tasktracker.model.Category;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskType;
import com.example.tasktracker.model.User;
import com.example.tasktracker.repository.CategoryRepository;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.repository.UserRepository;
import com.example.taskcontracts.event.TaskEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private TaskEventProducer taskEventProducer;

    @InjectMocks
    private TaskService taskService;

    private Task task;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).email("john@example.com").build();
        Category category = Category.builder().id(1L).name("Work").build();

        task = Task.builder()
                .id(1L)
                .title("Do work")
                .deadline(LocalDateTime.of(2026, 3, 20, 12, 0))
                .status("TODO")
                .type(TaskType.OTHER)
                .user(user)
                .category(category)
                .build();
    }

    @Test
    void createTask() {
        CreateTaskDto dto = new CreateTaskDto("Do work", null, null, "TODO", null, 1L, 1L);
        when(taskMapper.toEntity(dto)).thenReturn(task);
        when(userRepository.findById(1L)).thenReturn(Optional.of(task.getUser()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(task.getCategory()));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toDto(task)).thenReturn(new TaskDto(1L, "Do work", null, task.getDeadline(), "TODO",
                TaskType.OTHER, 1L, 1L));

        TaskDto result = taskService.createTask(dto);

        assertNotNull(result);
        assertEquals("Do work", result.title());
        assertEquals(TaskType.OTHER, result.type());
        assertEquals(1L, result.userId());
        assertEquals(1L, result.categoryId());
        verify(taskMapper).toEntity(dto);
        verify(taskMapper).toDto(task);
        verify(taskEventProducer).sendTaskEvent(TaskEventType.CREATED, task);
    }

    @Test
    void getTasks_WithFiltersAndPaging() {
        PageRequest pageRequest = PageRequest.of(0, 5, org.springframework.data.domain.Sort.Direction.DESC, "deadline");
        when(taskRepository.searchTasks(
                eq("Work"),
                eq(TaskType.WORK),
                eq("IN_PROGRESS"),
                eq(LocalDate.of(2026, 3, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 3, 31).plusDays(1).atStartOfDay().minusNanos(1)),
                eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(task), pageRequest, 1));
        when(taskMapper.toDto(task)).thenReturn(new TaskDto(1L, "Do work", null, task.getDeadline(), "TODO",
                TaskType.OTHER, 1L, 1L));

        Page<TaskDto> results = taskService.getTasks(
                "Work",
                TaskType.WORK,
                "IN_PROGRESS",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                0,
                5,
                "deadline",
                "desc");

        assertEquals(1, results.getTotalElements());
        assertEquals("Do work", results.getContent().getFirst().title());
        verify(taskRepository).searchTasks(
                "Work",
                TaskType.WORK,
                "IN_PROGRESS",
                LocalDate.of(2026, 3, 1).atStartOfDay(),
                LocalDate.of(2026, 3, 31).plusDays(1).atStartOfDay().minusNanos(1),
                pageRequest);
    }

    @Test
    void getTasks_WhenDateRangeInvalid() {
        assertThrows(ValidationException.class, () -> taskService.getTasks(
                null,
                null,
                null,
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 1),
                0,
                10,
                "id",
                "asc"));
    }

    @Test
    void getTaskById() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toDto(task)).thenReturn(new TaskDto(1L, "Do work", null, task.getDeadline(), "TODO",
                TaskType.OTHER, 1L, 1L));

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
        task.setType(TaskType.WORK);
        TaskDto dto = new TaskDto(null, "Updated title", "Updated description", null, "IN_PROGRESS", null, 2L, 3L);
        User updatedUser = User.builder().id(2L).email("updated@example.com").build();
        Category updatedCategory = Category.builder().id(3L).name("Home").build();
        Task updatedTask = Task.builder()
                .id(1L)
                .title("Updated title")
                .description("Updated description")
                .status("IN_PROGRESS")
                .type(TaskType.WORK)
                .user(updatedUser)
                .category(updatedCategory)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(updatedUser));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(updatedCategory));
        when(taskRepository.save(task)).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask))
                .thenReturn(new TaskDto(1L, "Updated title", "Updated description", null, "IN_PROGRESS",
                        TaskType.WORK, 2L, 3L));

        TaskDto result = taskService.updateTask(1L, dto);

        assertEquals("Updated title", result.title());
        assertEquals("IN_PROGRESS", result.status());
        assertEquals(TaskType.WORK, result.type());
        assertEquals(2L, result.userId());
        assertEquals(3L, result.categoryId());
        verify(taskEventProducer).sendTaskEvent(TaskEventType.UPDATED, updatedTask);
    }

    @Test
    void updateTask_WhenNotFound() {
        TaskDto dto = new TaskDto(null, "Updated", null, null, "DONE", null, 1L, 1L);
        when(taskRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(100L, dto));
    }

    @Test
    void updateTask_WhenUserNotFound() {
        TaskDto dto = new TaskDto(null, "Updated title", "Updated description", null, "IN_PROGRESS", null, 99L, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(1L, dto));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_WhenCategoryNotFound() {
        TaskDto dto = new TaskDto(null, "Updated title", "Updated description", null, "IN_PROGRESS", null, null, 99L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(1L, dto));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_WhenExists() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertDoesNotThrow(() -> taskService.deleteTask(1L));
        verify(taskRepository, times(1)).delete(task);
        verify(taskEventProducer).sendTaskEvent(TaskEventType.DELETED, task);
    }

    @Test
    void deleteTask_WhenNotExists() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(1L));
        verify(taskRepository, never()).delete(any(Task.class));
    }
}
