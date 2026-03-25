package com.observability.userservice.service;

import com.observability.userservice.dto.CreateUserDTO;
import com.observability.userservice.dto.UserResponseDTO;
import com.observability.userservice.exceptions.DuplicateEmailException;
import com.observability.userservice.exceptions.InvalidPageException;
import com.observability.userservice.exceptions.UserNotFoundException;
import com.observability.userservice.model.User;
import com.observability.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "maxPageSize", 50);
    }

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

    @Test
    void getAllUsersPaginatedShouldReturnMappedPage() {
        User firstUser = new User("Alice", "alice@example.com");
        User secondUser = new User("Bob", "bob@example.com");
        ReflectionTestUtils.setField(firstUser, "userId", 1L);
        ReflectionTestUtils.setField(secondUser, "userId", 2L);

        Page<User> userPage = new PageImpl<>(
                List.of(firstUser, secondUser),
                PageRequest.of(0, 2),
                2
        );

        when(userRepository.findAll(argThat((Pageable pageable) ->
                pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 2
                        && pageable.getSort().getOrderFor("userId") != null
                        && pageable.getSort().getOrderFor("userId").isAscending()
        ))).thenReturn(userPage);

        Page<UserResponseDTO> response = userService.getAllUsersPaginated(0, 2);

        assertEquals(2, response.getContent().size());
        assertEquals("Alice", response.getContent().get(0).userName());
        assertEquals("bob@example.com", response.getContent().get(1).email());
        verify(userRepository).findAll(argThat((Pageable pageable) ->
                pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 2
                        && pageable.getSort().getOrderFor("userId") != null
                        && pageable.getSort().getOrderFor("userId").isAscending()
        ));
    }

    @Test
    void getAllUsersPaginatedShouldThrowWhenPageIsNegative() {
        assertThrows(InvalidPageException.class, () -> userService.getAllUsersPaginated(-1, 20));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllUsersPaginatedShouldThrowWhenPageSizeIsZero() {
        assertThrows(InvalidPageException.class, () -> userService.getAllUsersPaginated(0, 0));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllUsersPaginatedShouldThrowWhenPageSizeExceedsCap() {
        assertThrows(InvalidPageException.class, () -> userService.getAllUsersPaginated(0, 51));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }
}
