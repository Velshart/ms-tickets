package me.mmtr.userservice.controller;

import me.mmtr.userservice.assembler.UserModelAssembler;
import me.mmtr.userservice.configuration.filter.RateLimitingFilter;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitingFilter.class
        ))
@Import(UserModelAssembler.class)
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
    private UserResponse userResponse;

    @BeforeEach
    public void setup() {
        user = new User(
                USERNAME,
                EMAIL,
                PASSWORD,
                Role.USER
        );
        user.setId(ID);

        userResponse = UserResponse.fromUser(user);
    }

    @Nested
    class GetAllUsersTests {

        @Test
        @WithMockUser
        void shouldReturnPagedUsersWithHateoasLinks() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UserResponse> userPage = new PageImpl<>(List.of(userResponse), pageable, 1);

            when(userService.listUsers(any(Pageable.class))).thenReturn(userPage);

            mockMvc.perform(get("/api/users")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.users[0].id").value(ID))
                    .andExpect(jsonPath("$._embedded.users[0].username").value(USERNAME))
                    .andExpect(jsonPath("$._embedded.users[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.page.totalPages").value(1));
        }
    }

    @Nested
    class GetUserByIdTests {

        @Test
        @WithMockUser
        void shouldReturnOkWithUserDataAndHateoasLinks() throws Exception {
            when(userService.getById(ID)).thenReturn(userResponse);

            mockMvc.perform(get("/api/users/" + ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.role").value(user.getRole().toString()))
                    .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/users/" + ID))
                    .andExpect(jsonPath("$._links.users.href").value("http://localhost/api/users"))
                    .andExpect(jsonPath("$._links.delete.href").value("http://localhost/api/users/" + ID));
        }

        @Test
        @WithMockUser
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
        @WithMockUser(roles = "SUPER_ADMIN")
        void shouldReturnCreatedWithTokenAfterSuccessfulRegistration() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest(
                    USERNAME,
                    EMAIL,
                    PASSWORD
            );

            Long userId = 1L;
            UserResponse adminResponse = new UserResponse(
                    userId,
                    USERNAME,
                    EMAIL,
                    Role.ADMIN.toString()
            );

            when(authenticationService.register(registerRequest, Role.ADMIN))
                    .thenReturn(adminResponse);

            mockMvc.perform(post("/api/users/register-admin")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.username").value(USERNAME))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.role").value(Role.ADMIN.toString()))
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest(
                    USERNAME,
                    EMAIL,
                    PASSWORD
            );

            when(authenticationService.register(registerRequest, Role.ADMIN))
                    .thenThrow(IllegalArgumentException.class);

            mockMvc.perform(post("/api/users/register-admin")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void shouldReturnBadRequestWhenBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/users/register-admin")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class DeleteUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturnNoContentAfterSuccessfulDeletion() throws Exception {
            mockMvc.perform(delete("/api/users/" + ID).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturnForbiddenWhenInsufficientPrivileges() throws Exception {
            doThrow(InsufficientPrivilegesException.class)
                    .when(userService).deleteUser(anyLong(), any());

            mockMvc.perform(delete("/api/users/" + ID).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}