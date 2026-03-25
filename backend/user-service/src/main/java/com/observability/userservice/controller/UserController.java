package com.observability.userservice.controller;

import com.observability.userservice.dto.CreateUserDTO;
import com.observability.userservice.dto.UserResponseDTO;
import com.observability.userservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserDTO createUser)  {
        log.info("Received create user request");
        UserResponseDTO createdUser = userService.createUser(createUser);
        log.info("Create user request completed successfully with userId={}", createdUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable long id){
        log.info("Received get user request for ID {}", id);
        UserResponseDTO getUser = userService.getUser(id);
        log.info("Get User request completed successfully for ID {}", id);
        return ResponseEntity.ok(getUser);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getAllUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Received get all user paginated request for page {} size {}", page, size);
        Page<UserResponseDTO> users = userService.getAllUsersPaginated(page, size);
        return ResponseEntity.ok(users);
    }
}
