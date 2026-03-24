package com.observability.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.observability.userservice.dto.CreateUserDTO;
import com.observability.userservice.dto.UserResponseDTO;
import com.observability.userservice.exceptions.DuplicateEmailException;
import com.observability.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
}
