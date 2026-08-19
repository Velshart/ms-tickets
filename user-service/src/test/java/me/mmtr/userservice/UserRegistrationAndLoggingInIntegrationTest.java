package me.mmtr.userservice;

import me.mmtr.userservice.model.dto.request.AuthenticationRequest;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserRegistrationAndLoggingInIntegrationTest extends UserServiceIntegrationTest {

    @Test
    void shouldCorrectlyRegisterNewAccountAndAuthenticate() throws Exception {
        String username = "user";
        String password = "password";
        RegisterRequest registerRequest =
                new RegisterRequest(username, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthenticationRequest authenticationRequest =
                new AuthenticationRequest(username, password);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationRequest)))
                .andExpect(status().isOk());
    }
}