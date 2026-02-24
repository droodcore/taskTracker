package com.example.tasktracker.mapper;

import com.example.tasktracker.dto.CategoryDto;
import com.example.tasktracker.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    @Mapping(target = "tasks", ignore = true)
    Category toEntity(CategoryDto dto);
}
