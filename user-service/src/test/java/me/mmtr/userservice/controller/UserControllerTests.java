package me.mmtr.userservice.controller;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.model.exception.InsufficientPrivilegesException;
import me.mmtr.userservice.model.exception.UserNotFoundException;
import me.mmtr.userservice.service.AuthenticationService;
import me.mmtr.userservice.service.JwtService;
import me.mmtr.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTests {
    private static final String USERNAME = "test_user";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password";
    private final long ID = 1L;
    @MockitoBean
    private AuthenticationService authenticationService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private User user;

    @BeforeEach
    public void setup() {
        user = new User(
                USERNAME,
                EMAIL,
                PASSWORD,
                Role.USER
        );
        user.setId(ID);
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void shouldReturnOkWithUserData() throws Exception {
            when(userService.getById(ID))
                    .thenReturn(UserResponse.fromUser(user));

            mockMvc.perform(get("/api/users/" + ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.role").value(user.getRole().toString()));
        }

        @Test
        void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
            long userId = 1L;

            when(userService.getById(userId))
                    .thenThrow(UserNotFoundException.class);

            mockMvc.perform(get("/api/users/" + userId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class RegisterAdminTests {
        @Test
        void shouldReturnCreatedWithTokenAfterSuccessfulRegistration() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest(
                    USERNAME,
                    EMAIL,
                    PASSWORD
            );

            Long userId = 1L;
            when(authenticationService.register(registerRequest, Role.ADMIN))
                    .thenReturn(new UserResponse(
                            userId,
                            USERNAME,
                            EMAIL,
                            Role.ADMIN.toString()
                    ));

            mockMvc.perform(post("/api/users/register-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.username").value(USERNAME))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.role").value(Role.ADMIN.toString()));
        }

        @Test
        void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest(
                    USERNAME,
                    EMAIL,
                    PASSWORD
            );

            when(authenticationService.register(registerRequest, Role.ADMIN))
                    .thenThrow(IllegalArgumentException.class);

            mockMvc.perform(post("/api/users/register-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldReturnBadRequestWhenBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/users/register-admin")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class DeleteUserTests {
        @Test
        void shouldReturnNoContentAfterSuccessfulDeletion() throws Exception {

            mockMvc.perform(delete("/api/users/" + ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        void shouldReturnForbiddenWhenInsufficientPrivileges() throws Exception {
            doThrow(InsufficientPrivilegesException.class)
                    .when(userService).deleteUser(anyLong(), any(User.class));

            mockMvc.perform(delete("/api/users/" + ID))
                    .andExpect(status().isForbidden());
        }
    }
}
