package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /api/users  ->  201 Created
    @PostMapping
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {

        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET /api/users/by-email?email=andrea@mail.com
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(
            @RequestParam String email) {

        return ResponseEntity.ok(userService.findByEmail(email));
    }

    // GET /api/users/by-username?username=andrea
    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(
            @RequestParam String username) {

        return ResponseEntity.ok(userService.findByUsername(username));
    }
}
