package com.example.tasktracker.service;

import com.example.tasktracker.dto.UserDto;
import com.example.tasktracker.mapper.UserMapper;
import com.example.tasktracker.model.User;
import com.example.tasktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDto createUser(UserDto userDto) {
        User user = userMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }
}
