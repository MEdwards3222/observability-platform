package com.observability.userservice.dto;

import java.time.Instant;

public record CreateUserDTO(String userName, String email) {
}
