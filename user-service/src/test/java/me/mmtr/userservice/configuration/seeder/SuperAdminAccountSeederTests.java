package me.mmtr.userservice.configuration.seeder;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.repository.UserRepository;
import me.mmtr.userservice.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SuperAdminAccountSeederTests {
    private final String USERNAME = "admin";
    private final String PASSWORD = "password123";
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationArguments applicationArguments;
    private SuperAdminAccountSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new SuperAdminAccountSeeder(
                authenticationService,
                userRepository,
                USERNAME,
                PASSWORD
        );
    }

    @Test
    void shouldCreateSuperAdminAccountIfItDoesNotExist() {
        when(userRepository.existsByUsername(USERNAME))
                .thenReturn(false);

        seeder.run(applicationArguments);

        ArgumentCaptor<RegisterRequest> responseCaptor =
                ArgumentCaptor.captor();
        verify(authenticationService).register(
                responseCaptor.capture(),
                eq(Role.SUPER_ADMIN)
        );

        RegisterRequest registerRequest = responseCaptor.getValue();
        assertEquals(USERNAME, registerRequest.username());
        assertEquals(PASSWORD, registerRequest.password());
    }

    @Test
    void shouldNotCreateSuperAdminAccountIfItAlreadyExists() {
        when(userRepository.existsByUsername(USERNAME))
                .thenReturn(true);

        seeder.run(applicationArguments);

        verifyNoInteractions(authenticationService);
    }
}
