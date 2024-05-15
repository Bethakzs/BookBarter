package com.example.userservice;

import com.example.userservice.dao.UserDAO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.entity.User;
import com.example.userservice.kafka.ReplyProcessor;
import com.example.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserServiceTest {

	@Mock
	private UserDAO userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@Mock
	private ReplyProcessor replyProcessor;

	private UserService userService;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		userService = new UserService(userRepository, passwordEncoder, kafkaTemplate, replyProcessor);
	}

	@Test
	public void testFindByEmailWithOutCheck() {
		User user = new User();
		user.setEmail("test@example.com");
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		User result = userService.findByEmailWithOutCheck("test@example.com");

		assertEquals("test@example.com", result.getEmail());
	}

	@Test
	public void testUpdateUser() {
		User user = new User();
		user.setEmail("test@example.com");
		when(userRepository.save(any(User.class))).thenReturn(user);

		userService.updateUser(user);

		verify(userRepository).save(user);
	}

	@Test
	public void testDeleteUser() {
		doNothing().when(userRepository).deleteByEmail(anyString());

		userService.deleteUser("test@example.com");

		verify(userRepository).deleteByEmail("test@example.com");
	}

	@Test
	public void testFindByEmailForCheck() {
		User user = new User();
		user.setEmail("test@example.com");
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		Optional<User> result = userService.findByEmailForCheck("test@example.com");

		assertTrue(result.isPresent());
		assertEquals("test@example.com", result.get().getEmail());
	}

	@Test
	public void testAddBucksToUser() {
		User user = new User();
		user.setEmail("test@example.com");
		user.setBucks(100L);
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenReturn(user);

		userService.addBucksToUser("test@example.com", 50L);

		verify(userRepository).save(user);
		assertEquals(150L, user.getBucks());
	}
}