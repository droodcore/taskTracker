package com.example.tasktracker.mapper;

import com.example.tasktracker.dto.CategoryDto;
import com.example.tasktracker.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    CategoryDto toDto(Category category);

    @Mapping(target = "tasks", ignore = true)
    Category toEntity(CategoryDto dto);
}
