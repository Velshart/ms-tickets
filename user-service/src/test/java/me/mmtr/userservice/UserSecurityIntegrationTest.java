package me.mmtr.userservice;

import me.mmtr.userservice.model.dto.request.AuthenticationRequest;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.AuthenticationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserSecurityIntegrationTest extends UserServiceModuleIntegrationTest {

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

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                username,
                password
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String userToken = objectMapper.readValue(
                loginResult.getResponse().getContentAsByteArray(),
                AuthenticationResponse.class
        ).token();

        RegisterRequest registerAdminRequest = new RegisterRequest(
                "admin",
                "admin@example.com",
                "password"
        );

        mockMvc.perform(post("/api/users/register-admin")
                        .with(csrf())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + userToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerAdminRequest)))
                .andExpect(status().isForbidden());
    }
}
