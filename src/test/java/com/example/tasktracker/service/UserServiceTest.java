package com.example.tasktracker.service;

import com.example.tasktracker.dto.UserDto;
import com.example.tasktracker.exception.ResourceNotFoundException;
import com.example.tasktracker.mapper.UserMapper;
import com.example.tasktracker.model.User;
import com.example.tasktracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("john").email("john@example.com").build();
    }

    @Test
    void createUser() {
        UserDto dto = new UserDto(null, "john", "john@example.com");
        when(userMapper.toEntity(dto)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(new UserDto(1L, "john", "john@example.com"));

        UserDto result = userService.createUser(dto);

        assertNotNull(result);
        assertEquals("john", result.username());
        verify(userMapper).toEntity(dto);
        verify(userMapper).toDto(user);
    }

    @Test
    void getAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toDto(user)).thenReturn(new UserDto(1L, "john", "john@example.com"));

        List<UserDto> results = userService.getAllUsers();

        assertEquals(1, results.size());
        assertEquals("john", results.get(0).username());
        verify(userMapper).toDto(user);
    }

    @Test
    void getUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(new UserDto(1L, "john", "john@example.com"));

        UserDto result = userService.getUserById(1L);

        assertEquals(1L, result.id());
        assertEquals("john", result.username());
    }

    @Test
    void getUserById_WhenNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(10L));
    }

    @Test
    void updateUser() {
        UserDto dto = new UserDto(null, "john-updated", "john-updated@example.com");
        User updated = User.builder().id(1L).username("john-updated").email("john-updated@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(updated);
        when(userMapper.toDto(updated)).thenReturn(new UserDto(1L, "john-updated", "john-updated@example.com"));

        UserDto result = userService.updateUser(1L, dto);

        assertEquals("john-updated", result.username());
        assertEquals("john-updated@example.com", result.email());
    }

    @Test
    void deleteUser_WhenExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_WhenNotFound() {
        when(userRepository.existsById(20L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(20L));
        verify(userRepository, never()).deleteById(20L);
    }
}
