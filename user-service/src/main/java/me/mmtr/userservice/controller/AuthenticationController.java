package me.mmtr.userservice.controller;

import jakarta.validation.Valid;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.dto.request.AuthenticationRequest;
import me.mmtr.userservice.model.dto.response.AuthenticationResponse;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authenticationService.register(
                        registerRequest,
                        Role.USER
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginUser(
            @Valid @RequestBody AuthenticationRequest authenticationRequest
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(authenticationService.login(authenticationRequest));
    }
}
