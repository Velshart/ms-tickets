package me.mmtr.userservice.service;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.messaging.UserCreatedEvent;
import me.mmtr.userservice.model.dto.request.AuthenticationRequest;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.AuthenticationResponse;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTests {
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Nested
    class RegisterTests {
        @Test
        void shouldRegisterNewUser() {
            String jwtToken = "valid.jwt.token";
            RegisterRequest registerRequest = new RegisterRequest(
                    "testUser",
                    "test@example.com",
                    "password"
            );
            String encodedPassword = "encodedPassword";

            when(userRepository.existsByUsername(registerRequest.username()))
                    .thenReturn(false);
            when(passwordEncoder.encode(registerRequest.password()))
                    .thenReturn(encodedPassword);

            User savedUser = new User(
                    registerRequest.username(),
                    registerRequest.email(),
                    registerRequest.password(),
                    Role.USER
            );
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            when(jwtService.generateToken(any(User.class)))
                    .thenReturn(jwtToken);

            AuthenticationResponse response = authenticationService.register(
                    registerRequest,
                    Role.USER
            );

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.captor();
            verify(userRepository).save(userCaptor.capture());

            User capturedUser = userCaptor.getValue();
            assertEquals(registerRequest.username(), capturedUser.getUsername());
            assertEquals(encodedPassword, capturedUser.getPassword());
            assertEquals(Role.USER, capturedUser.getRole());

            ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.captor();
            verify(publisher).publishEvent(eventCaptor.capture());

            UserCreatedEvent capturedEvent = eventCaptor.getValue();
            assertEquals(savedUser.getId(), capturedEvent.id());
            assertEquals(savedUser.getEmail(), capturedEvent.email());

            assertEquals(jwtToken, response.token());
        }

        @Test
        void shouldThrowExceptionWhenUsernameAlreadyExists() {
            RegisterRequest registerRequest = new RegisterRequest(
                    "testUser",
                    "test@example.com",
                    "password"
            );

            when(userRepository.existsByUsername(registerRequest.username()))
                    .thenReturn(true);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> authenticationService
                            .register(registerRequest, Role.USER)
            );

            verifyNoMoreInteractions(userRepository);
        }
    }

    @Nested
    class LoginTests {
        @Test
        void shouldAuthenticateUserWhenValidCredentialsWereProvided() {
            String jwtToken = "valid.jwt.token";
            AuthenticationRequest authenticationRequest =
                    new AuthenticationRequest(
                            "testUser",
                            "password"
                    );

            User user = new User(
                    "testUser",
                    "test@example.com",
                    "password",
                    Role.USER
            );

            when(userRepository.findByUsername(authenticationRequest.username()))
                    .thenReturn(Optional.of(user));

            when(jwtService.generateToken(user)).thenReturn(jwtToken);

            AuthenticationResponse authenticationResponse =
                    authenticationService.login(authenticationRequest);

            assertEquals(jwtToken, authenticationResponse.token());
        }

        @Test
        void shouldThrowExceptionWhenAuthenticationFails() {
            AuthenticationRequest authenticationRequest =
                    new AuthenticationRequest(
                            "testUser",
                            "password"
                    );

            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.username(),
                            authenticationRequest.password()
                    );

            doThrow(BadCredentialsException.class)
                    .when(authenticationManager)
                    .authenticate(token);

            assertThrows(
                    BadCredentialsException.class,
                    () -> authenticationService.login(authenticationRequest)
            );

            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldThrowExceptionWhenUsernameNotFound() {
            AuthenticationRequest authenticationRequest =
                    new AuthenticationRequest(
                            "testUser",
                            "password"
                    );
            when(userRepository.findByUsername(authenticationRequest.username()))
                    .thenReturn(Optional.empty());

            assertThrows(
                    UsernameNotFoundException.class,
                    () -> authenticationService.login(authenticationRequest)
            );
        }
    }
}
