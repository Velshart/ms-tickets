package me.mmtr.userservice;

import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminSeederIntegrationTest extends UserServiceIntegrationTest {
    @Autowired
    protected UserRepository userRepository;
    @Value("${super.admin.username}")
    private String superAdminUsername;

    @Test
    void shouldSeedAdminAccountOnStartup() {
        assertTrue(userRepository.existsByUsername(superAdminUsername));
    }
}
