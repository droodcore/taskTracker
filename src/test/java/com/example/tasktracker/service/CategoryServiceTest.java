package com.example.tasktracker.service;

import com.example.tasktracker.dto.CategoryDto;
import com.example.tasktracker.mapper.CategoryMapper;
import com.example.tasktracker.model.Category;
import com.example.tasktracker.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Work").build();
    }

    @Test
    void createCategory() {
        CategoryDto dto = new CategoryDto(null, "Work");
        when(categoryMapper.toEntity(dto)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(new CategoryDto(1L, "Work"));

        CategoryDto result = categoryService.createCategory(dto);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Work", result.name());
        verify(categoryMapper).toEntity(dto);
        verify(categoryMapper).toDto(category);
    }

    @Test
    void getAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toDto(category)).thenReturn(new CategoryDto(1L, "Work"));

        List<CategoryDto> results = categoryService.getAllCategories();

        assertEquals(1, results.size());
        assertEquals("Work", results.get(0).name());
        verify(categoryMapper).toDto(category);
    }
}
