package com.example.tasktracker.repository;

import com.example.tasktracker.model.Category;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskType;
import com.example.tasktracker.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void searchTasks_FiltersAndPaginates() {
        User user = userRepository.save(User.builder()
                .username("repo-user")
                .email("repo-user@example.com")
                .build());

        Category work = categoryRepository.save(Category.builder().name("Repo Work").build());
        Category home = categoryRepository.save(Category.builder().name("Repo Home").build());

        taskRepository.save(Task.builder()
                .title("Work task")
                .status("IN_PROGRESS")
                .deadline(LocalDateTime.of(2026, 3, 20, 10, 0))
                .type(TaskType.WORK)
                .user(user)
                .category(work)
                .build());

        taskRepository.save(Task.builder()
                .title("Home task")
                .status("DONE")
                .deadline(LocalDateTime.of(2026, 4, 5, 10, 0))
                .type(TaskType.OTHER)
                .user(user)
                .category(home)
                .build());

        Page<Task> result = taskRepository.searchTasks(
                "Repo Work",
                TaskType.WORK,
                "IN_PROGRESS",
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59, 59),
                PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals("Work task", result.getContent().getFirst().getTitle());
    }
}
