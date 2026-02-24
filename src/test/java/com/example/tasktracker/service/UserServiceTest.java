package com.example.tasktracker.service;

import com.example.tasktracker.dto.UserDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
}
