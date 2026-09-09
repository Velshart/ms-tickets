package me.mmtr.userservice.controller;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.dto.request.AuthenticationRequest;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.AuthenticationResponse;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.service.AuthenticationService;
import me.mmtr.userservice.service.JwtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
public class AuthenticationControllerTests {
    private static final String USERNAME = "test_user";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "generated.jwt.token";
    @MockitoBean
    private AuthenticationService authenticationService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class RegisterUserTests {
        @Test
        void shouldReturnCreatedWithTokenAfterSuccessfulRegistration() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest(
                    USERNAME,
                    EMAIL,
                    PASSWORD
            );

            Long userId = 1L;
            when(authenticationService.register(registerRequest, Role.USER))
                    .thenReturn(new UserResponse(
                            userId,
                            USERNAME,
                            EMAIL,
                            Role.USER.toString()
                    ));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper
                                            .writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.username").value(USERNAME))
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.role").value(Role.USER.toString()));
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnBadRequestWhenBlankDataProvided() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest(
                    "",
                    "",
                    ""
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper
                                            .writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest());

            verify(authenticationService, never()).register(any(), any());
        }

        @Test
        void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest(
                    USERNAME,
                    EMAIL,
                    PASSWORD
            );

            when(authenticationService.register(registerRequest, Role.USER))
                    .thenThrow(IllegalArgumentException.class);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper
                                            .writeValueAsString(registerRequest)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class LoginTests {
        @Test
        void shouldReturnOkWithTokenAfterSuccessfulLogin() throws Exception {
            AuthenticationRequest authenticationRequest =
                    new AuthenticationRequest(
                            USERNAME,
                            PASSWORD
                    );

            when(authenticationService.login(authenticationRequest))
                    .thenReturn(new AuthenticationResponse(TOKEN));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper
                                    .writeValueAsString(authenticationRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN));
        }

        @Test
        void shouldReturnBadRequestWhenBlandDataProvided() throws Exception {
            AuthenticationRequest authenticationRequest =
                    new AuthenticationRequest(
                            "",
                            ""
                    );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper
                                    .writeValueAsString(authenticationRequest)))
                    .andExpect(status().isBadRequest());

            verify(authenticationService, never()).login(any());
        }

        @Test
        void shouldReturnUnauthorizedWhenInvalidCredentialsProvided() throws Exception {
            AuthenticationRequest authenticationRequest =
                    new AuthenticationRequest(
                            USERNAME,
                            PASSWORD
                    );

            when(authenticationService.login(authenticationRequest))
                    .thenThrow(BadCredentialsException.class);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper
                                    .writeValueAsString(authenticationRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
