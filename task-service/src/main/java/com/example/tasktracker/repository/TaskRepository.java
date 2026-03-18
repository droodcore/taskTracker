package com.example.tasktracker.repository;

import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByUserId(Long userId, Pageable pageable);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findByCategoryNameIgnoreCase(String categoryName);

    List<Task> findByType(TaskType type);

    List<Task> findByCategoryNameIgnoreCaseAndType(String categoryName, TaskType type);

    @Query(
            value = """
                    select t
                    from Task t
                    join t.category c
                    where (:category is null or lower(c.name) = lower(:category))
                      and (:type is null or t.type = :type)
                      and (:status is null or lower(t.status) = lower(:status))
                      and (:deadlineFrom is null or t.deadline >= :deadlineFrom)
                      and (:deadlineTo is null or t.deadline <= :deadlineTo)
                    """,
            countQuery = """
                    select count(t)
                    from Task t
                    join t.category c
                    where (:category is null or lower(c.name) = lower(:category))
                      and (:type is null or t.type = :type)
                      and (:status is null or lower(t.status) = lower(:status))
                      and (:deadlineFrom is null or t.deadline >= :deadlineFrom)
                      and (:deadlineTo is null or t.deadline <= :deadlineTo)
                    """
    )
    Page<Task> searchTasks(
            @Param("category") String category,
            @Param("type") TaskType type,
            @Param("status") String status,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo,
            Pageable pageable);
}
