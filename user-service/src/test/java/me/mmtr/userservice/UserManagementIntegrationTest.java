package me.mmtr.userservice;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserManagementIntegrationTest extends UserServiceModuleIntegrationTest {
    @Autowired
    protected UserRepository userRepository;
    @Value("${super.admin.username}")
    private String superAdminUsername;

    @Test
    void shouldDeleteUserAndRemoveItFromDatabase() throws Exception {
        String username = "user_to_delete";
        RegisterRequest registerRequest = new RegisterRequest(
                username,
                "test@example.com",
                "password"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        User savedUser = userRepository.findByUsername(username).orElseThrow();
        User superAdmin = userRepository.findByUsername(superAdminUsername).orElseThrow();
        mockMvc.perform(delete("/api/users/" + savedUser.getId())
                        .header("X-User-Id", superAdmin.getId().toString())
                        .header("X-User-Name", superAdmin.getUsername())
                        .header("X-User-Email", superAdmin.getEmail())
                        .header("X-User-Role", "ROLE_" + Role.SUPER_ADMIN.name()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsByUsername(username));
    }
}
