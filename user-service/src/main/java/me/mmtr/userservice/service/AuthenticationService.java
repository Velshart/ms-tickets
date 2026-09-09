package me.mmtr.userservice.service;

import jakarta.transaction.Transactional;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.messaging.UserCreatedEvent;
import me.mmtr.userservice.model.dto.request.AuthenticationRequest;
import me.mmtr.userservice.model.dto.response.AuthenticationResponse;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher publisher;

    public AuthenticationService(
            UserRepository userRepository,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.publisher = applicationEventPublisher;
    }

    @Transactional
    public UserResponse register(
            RegisterRequest registerRequest,
            Role role
    ) {
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new IllegalArgumentException("Username is already in use");
        }

        User user = new User(
                registerRequest.username(),
                registerRequest.email(),
                passwordEncoder.encode(registerRequest.password()),
                role
        );

        User savedUser = userRepository.save(user);

        publisher.publishEvent(new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getEmail()
        ));
        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().toString()
        );
    }

    public AuthenticationResponse login(
            AuthenticationRequest authenticationRequest
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.username(),
                        authenticationRequest.password()
                )
        );

        User user = userRepository
                .findByUsername(authenticationRequest.username())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Invalid login credentials provided"
                ));

        return new AuthenticationResponse(jwtService.generateToken(user));
    }
}
