package me.mmtr.userservice.configuration.seeder;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.repository.UserRepository;
import me.mmtr.userservice.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SuperAdminAccountSeeder implements ApplicationRunner {
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    private final String adminAccountUsername;
    private final String adminAccountEmail;
    private final String adminAccountPassword;

    public SuperAdminAccountSeeder(
            AuthenticationService authenticationService,
            UserRepository userRepository,
            @Value("${super.admin.username}") String adminAccountUsername,
            @Value("${super.admin.email}") String adminAccountEmail,
            @Value("${super.admin.password}") String adminAccountPassword
    ) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.adminAccountUsername = adminAccountUsername;
        this.adminAccountEmail = adminAccountEmail;
        this.adminAccountPassword = adminAccountPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        registerSuperAdminAccount();
    }

    private void registerSuperAdminAccount() {
        if (!userRepository.existsByUsername(adminAccountUsername)) {
            RegisterRequest registerRequest = new RegisterRequest(
                    adminAccountUsername,
                    adminAccountEmail,
                    adminAccountPassword
            );

            authenticationService.register(registerRequest, Role.SUPER_ADMIN);
        }
    }
}
