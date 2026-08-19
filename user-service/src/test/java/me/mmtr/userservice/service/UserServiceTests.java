package me.mmtr.userservice.service;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.model.exception.InsufficientPrivilegesException;
import me.mmtr.userservice.model.exception.UserNotFoundException;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_NAME = "testUser";
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        final String testUserPassword = "password";
        testUser = new User(
                TEST_USER_NAME,
                testUserPassword,
                Role.SUPER_ADMIN
        );
        testUser.setId(TEST_USER_ID);
    }

    @Nested
    class GetByIdTests {
        @Test
        void shouldReturnUserById() {
            when(userRepository.findById(TEST_USER_ID))
                    .thenReturn(Optional.of(testUser));

            UserResponse userResponse = userService.getById(TEST_USER_ID);

            assertEquals(TEST_USER_ID, userResponse.id());
            assertEquals(TEST_USER_NAME, userResponse.username());
            assertEquals(Role.SUPER_ADMIN.toString(), userResponse.role());
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(TEST_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.getById(TEST_USER_ID)
            );
        }
    }

    @Nested
    class ListUsersTests {
        @Test
        void shouldListExistingUsers() {
            when(userRepository.findAll())
                    .thenReturn(List.of(testUser));

            List<UserResponse> users = userService.listUsers();

            assertEquals(1, users.size());
            assertEquals(testUser.getId(), users.getFirst().id());
            assertEquals(testUser.getUsername(), users.getFirst().username());
            assertEquals(Role.SUPER_ADMIN.toString(), users.getFirst().role());
        }

        @Test
        void shouldReturnEmptyListWhenNoUserFound() {
            when(userRepository.findAll())
                    .thenReturn(Collections.emptyList());

            List<UserResponse> users = userService.listUsers();
            assertEquals(0, users.size());
        }
    }

    @Nested
    class DeleteUserTests {
        @Test
        void shouldDeleteUserById() {
            long targetId = 2L;
            User target = new User(
                    "testUser2",
                    "password",
                    Role.USER
            );
            target.setId(targetId);

            when(userRepository.findById(targetId))
                    .thenReturn(Optional.of(target));

            userService.deleteUser(targetId, testUser);

            verify(userRepository).delete(target);
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            long targetId = 2L;
            when(userRepository.findById(targetId))
                    .thenReturn(Optional.empty());

            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.deleteUser(targetId, testUser)
            );

            verify(userRepository, never()).delete(any());
        }

        @Test
        void shouldThrowExceptionWhenInsufficientPrivileges() {
            long requesterId = 2L;
            User requester = new User(
                    "testUser2",
                    "password",
                    Role.USER
            );
            requester.setId(requesterId);

            when(userRepository.findById(TEST_USER_ID))
                    .thenReturn(Optional.of(testUser));

            assertThrows(
                    InsufficientPrivilegesException.class,
                    () -> userService.deleteUser(TEST_USER_ID, requester)
            );

            verify(userRepository, never()).delete(any());
        }

        @Test
        void shouldThrowExceptionWhenUserTriesToDeleteThemselves() {
            when(userRepository.findById(TEST_USER_ID))
                    .thenReturn(Optional.of(testUser));

            assertThrows(
                    InsufficientPrivilegesException.class,
                    () -> userService.deleteUser(TEST_USER_ID, testUser)
            );

            verify(userRepository, never()).delete(any());
        }
    }
}
