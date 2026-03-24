package com.observability.userservice.service;

import com.observability.userservice.dto.CreateUserDTO;
import com.observability.userservice.dto.UserResponseDTO;
import com.observability.userservice.exceptions.DuplicateEmailException;
import com.observability.userservice.model.User;
import com.observability.userservice.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser(CreateUserDTO user) {
        String sanitizedEmail = user.email().trim().toLowerCase();

        if (userRepository.existsByEmail(sanitizedEmail)) {
            throw new DuplicateEmailException("Email already exists");
        }

        try {
            User newUser = new User(user.userName(), sanitizedEmail);
            User savedUser = userRepository.save(newUser);

            return new UserResponseDTO(savedUser.getUserId(), savedUser.getUserName(), savedUser.getEmail());
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException("Email already exists");
        }
    }
}