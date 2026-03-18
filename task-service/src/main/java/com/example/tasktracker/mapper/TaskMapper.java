package com.example.tasktracker.mapper;

import com.example.tasktracker.dto.CreateTaskDto;
import com.example.tasktracker.dto.TaskDto;
import com.example.tasktracker.model.Category;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "category.id", target = "categoryId")
    TaskDto toDto(Task task);

    @Mapping(source = "userId", target = "user")
    @Mapping(source = "categoryId", target = "category")
    Task toEntity(TaskDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "userId", target = "user")
    @Mapping(source = "categoryId", target = "category")
    Task toEntity(CreateTaskDto dto);

    default User mapUser(Long id) {
        if (id == null) {
            return null;
        }
        return User.builder().id(id).build();
    }

    default Category mapCategory(Long id) {
        if (id == null) {
            return null;
        }
        return Category.builder().id(id).build();
    }
}
