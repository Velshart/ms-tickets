package me.mmtr.userservice.controller;

import jakarta.validation.Valid;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.AuthenticationResponse;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.service.AuthenticationService;
import me.mmtr.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public UserController(
            AuthenticationService authenticationService,
            UserService userService
    ) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuthenticationResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest registerRequest) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authenticationService.register(
                        registerRequest, Role.ADMIN
                ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }
}