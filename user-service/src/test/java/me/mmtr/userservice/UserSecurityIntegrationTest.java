package me.mmtr.userservice;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserSecurityIntegrationTest extends UserServiceModuleIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void beforeEach() {
        rateLimitingFilter.reset();
    }

    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToRegisterAdmin() throws Exception {
        String username = "user";
        String email = "test@example.com";
        String password = "password";
        RegisterRequest registerRequest =
                new RegisterRequest(
                        username, email, password
                );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        User registeredUser = userRepository.findByUsername(username).orElseThrow();

        RegisterRequest registerAdminRequest = new RegisterRequest(
                "admin",
                "admin@example.com",
                "password"
        );

        mockMvc.perform(post("/api/users/register-admin")
                        .with(csrf())
                        .header("X-User-Id", registeredUser.getId().toString())
                        .header("X-User-Name", registeredUser.getUsername())
                        .header("X-User-Email", registeredUser.getEmail())
                        .header("X-User-Role", "ROLE_" + Role.USER.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerAdminRequest)))
                .andExpect(status().isForbidden());
    }
}
