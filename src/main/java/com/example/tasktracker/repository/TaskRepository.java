package com.example.tasktracker.repository;

import com.example.tasktracker.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByUserId(Long userId, Pageable pageable);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findByCategoryNameIgnoreCase(String categoryName);
}
