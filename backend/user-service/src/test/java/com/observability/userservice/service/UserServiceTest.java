package com.observability.userservice.service;

import com.observability.userservice.dto.CreateUserDTO;
import com.observability.userservice.dto.UserResponseDTO;
import com.observability.userservice.exceptions.DuplicateEmailException;
import com.observability.userservice.exceptions.UserNotFoundException;
import com.observability.userservice.model.User;
import com.observability.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserShouldSaveUserAndReturnResponse() {
        CreateUserDTO createUserDTO = new CreateUserDTO("Alice", " Alice@Example.com ");
        User savedUser = new User("Alice", "alice@example.com");
        ReflectionTestUtils.setField(savedUser, "userId", 1L);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDTO response = userService.createUser(createUserDTO);

        assertEquals(1L, response.userId());
        assertEquals("Alice", response.userName());
        assertEquals("alice@example.com", response.email());
        verify(userRepository).existsByEmail("alice@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserShouldThrowWhenEmailAlreadyExists() {
        CreateUserDTO createUserDTO = new CreateUserDTO("Alice", "alice@example.com");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(createUserDTO));
        verify(userRepository).existsByEmail("alice@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserShouldTranslateDatabaseConstraintViolation() {
        CreateUserDTO createUserDTO = new CreateUserDTO("Alice", "alice@example.com");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(createUserDTO));
        verify(userRepository).existsByEmail("alice@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserShouldReturnUserResponseWhenUserExists() {
        User user = new User("Alice", "alice@example.com");
        ReflectionTestUtils.setField(user, "userId", 1L);

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        UserResponseDTO response = userService.getUser(1L);

        assertEquals(1L, response.userId());
        assertEquals("Alice", response.userName());
        assertEquals("alice@example.com", response.email());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(99L));
        verify(userRepository).findById(99L);
    }
}
