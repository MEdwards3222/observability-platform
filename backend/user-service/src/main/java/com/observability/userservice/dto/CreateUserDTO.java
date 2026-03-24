package com.observability.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserDTO(
        @NotBlank(message = "User name is required")
        String userName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {
}
