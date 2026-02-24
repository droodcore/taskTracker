package com.example.tasktracker.service;

import com.example.tasktracker.dto.CategoryDto;
import com.example.tasktracker.mapper.CategoryMapper;
import com.example.tasktracker.model.Category;
import com.example.tasktracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category category = CategoryMapper.INSTANCE.toEntity(categoryDto);
        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.INSTANCE.toDto(savedCategory);
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryMapper.INSTANCE::toDto)
                .toList();
    }
}
