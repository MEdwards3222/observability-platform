package com.observability.userservice.service;

import com.observability.userservice.dto.CreateUserDTO;
import com.observability.userservice.dto.UserResponseDTO;
import com.observability.userservice.exceptions.DuplicateEmailException;
import com.observability.userservice.exceptions.UserNotFoundException;
import com.observability.userservice.model.User;
import com.observability.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser(CreateUserDTO user) {
        String sanitizedEmail = user.email().trim().toLowerCase();
        log.info("Attempting to create user with normalized email={}", sanitizedEmail);

        if (userRepository.existsByEmail(sanitizedEmail)) {
            log.warn("User creation rejected because email already exists: {}", sanitizedEmail);
            throw new DuplicateEmailException("Email already exists");
        }

        try {
            User newUser = new User(user.userName(), sanitizedEmail);
            User savedUser = userRepository.save(newUser);
            log.info("User created successfully with userId={}", savedUser.getUserId());

            return new UserResponseDTO(savedUser.getUserId(), savedUser.getUserName(), savedUser.getEmail());
        } catch (DataIntegrityViolationException e) {
            log.warn("User creation failed due to duplicate email detected at the database layer: {}", sanitizedEmail);
            throw new DuplicateEmailException("Email already exists");
        }
    }

    public UserResponseDTO getUser(Long id){
        log.info("Searching for user with ID {}", id);
        User searchedUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));

        return new UserResponseDTO(
                searchedUser.getUserId(),
                searchedUser.getUserName(),
                searchedUser.getEmail()
        );
    }
}
