package me.mmtr.userservice;

import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.request.AuthenticationRequest;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.AuthenticationResponse;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserManagementIntegrationTest extends UserServiceIntegrationTest {
    @Autowired
    protected UserRepository userRepository;
    @Value("${super.admin.username}")
    private String superAdminUsername;
    @Value("${super.admin.password}")
    private String superAdminPassword;

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

        AuthenticationRequest superAdminAuthenticationRequest =
                new AuthenticationRequest(superAdminUsername, superAdminPassword);

        MvcResult adminAuthenticationResult =
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                        .writeValueAsString(
                                                superAdminAuthenticationRequest
                                        )))
                        .andExpect(status().isOk())
                        .andReturn();

        String token = objectMapper.readValue(
                adminAuthenticationResult.getResponse().getContentAsByteArray(),
                AuthenticationResponse.class
        ).token();

        User savedUser = userRepository.findByUsername(username).orElseThrow();

        mockMvc.perform(delete("/api/users/" + savedUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsByUsername(username));
    }
}
