package com.example.tasktracker;

import com.example.tasktracker.mapper.CategoryMapper;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.mapper.UserMapper;
import com.example.tasktracker.repository.CategoryRepository;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
class TaskTrackerApplicationTests {

	@MockBean
	private CategoryRepository categoryRepository;

	@MockBean
	private TaskRepository taskRepository;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private CategoryMapper categoryMapper;

	@MockBean
	private TaskMapper taskMapper;

	@MockBean
	private UserMapper userMapper;

	@Test
	void contextLoads() {
	}

}
