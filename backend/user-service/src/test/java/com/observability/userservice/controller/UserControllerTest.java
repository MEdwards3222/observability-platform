package com.observability.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.observability.userservice.dto.CreateUserDTO;
import com.observability.userservice.dto.UserResponseDTO;
import com.observability.userservice.exceptions.DuplicateEmailException;
import com.observability.userservice.exceptions.InvalidPageException;
import com.observability.userservice.exceptions.UserNotFoundException;
import com.observability.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setValidator(validator)
                .build();
    }

    @Test
    void createUserShouldReturnCreatedResponse() throws Exception {
        CreateUserDTO request = new CreateUserDTO("Alice", "alice@example.com");
        UserResponseDTO response = new UserResponseDTO(1L, "Alice", "alice@example.com");

        when(userService.createUser(request)).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userName").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void createUserShouldReturnConflictForDuplicateEmail() throws Exception {
        CreateUserDTO request = new CreateUserDTO("Alice", "alice@example.com");

        when(userService.createUser(request)).thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUserShouldReturnBadRequestWhenUserNameIsBlank() throws Exception {
        CreateUserDTO request = new CreateUserDTO("   ", "alice@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void createUserShouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        CreateUserDTO request = new CreateUserDTO("Alice", "not-an-email");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void getUserShouldReturnOkResponse() throws Exception {
        UserResponseDTO response = new UserResponseDTO(1L, "Alice", "alice@example.com");

        when(userService.getUser(1L)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userName").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void getUserShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.getUser(99L)).thenThrow(new UserNotFoundException("User not found with id 99"));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsersPaginatedShouldReturnOkResponse() throws Exception {
        Page<UserResponseDTO> response = new PageImpl<>(
                List.of(
                        new UserResponseDTO(1L, "Alice", "alice@example.com"),
                        new UserResponseDTO(2L, "Bob", "bob@example.com")
                ),
                PageRequest.of(0, 2),
                2
        );

        when(userService.getAllUsersPaginated(0, 2)).thenReturn(response);

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[1].userName").value("Bob"))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllUsersPaginatedShouldReturnBadRequestWhenPageIsNegative() throws Exception {
        when(userService.getAllUsersPaginated(-1, 20))
                .thenThrow(new InvalidPageException("Page index must be 0 or greater"));

        mockMvc.perform(get("/users")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsersPaginatedShouldReturnBadRequestWhenSizeExceedsCap() throws Exception {
        when(userService.getAllUsersPaginated(0, 51))
                .thenThrow(new InvalidPageException("Page size must be between 1 and 50"));

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "51"))
                .andExpect(status().isBadRequest());
    }
}
