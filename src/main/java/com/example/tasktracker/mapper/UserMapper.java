package com.example.tasktracker.mapper;

import com.example.tasktracker.dto.UserDto;
import com.example.tasktracker.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);

    @Mapping(target = "tasks", ignore = true)
    User toEntity(UserDto dto);
}
