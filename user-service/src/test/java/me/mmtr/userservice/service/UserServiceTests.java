package me.mmtr.userservice.service;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.messaging.UserDeletedEvent;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.model.exception.InsufficientPrivilegesException;
import me.mmtr.userservice.model.exception.UserNotFoundException;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_NAME = "testUser";
    private static final String TEST_USER_EMAIL = "test@example.com";
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher publisher;
    @InjectMocks
    private UserService userService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        final String testUserPassword = "password";
        testUser = new User(
                TEST_USER_NAME,
                TEST_USER_EMAIL,
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
    class GetByUsernameTests {
        @Test
        void shouldReturnUserByUsername() {
            when(userRepository.findByUsername(TEST_USER_NAME))
                    .thenReturn(Optional.of(testUser));

            UserResponse userResponse = userService.getByUsername(TEST_USER_NAME);

            assertEquals(TEST_USER_ID, userResponse.id());
            assertEquals(TEST_USER_NAME, userResponse.username());
            assertEquals(Role.SUPER_ADMIN.toString(), userResponse.role());
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByUsername(TEST_USER_NAME))
                    .thenReturn(Optional.empty());

            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.getByUsername(TEST_USER_NAME)
            );
        }
    }

    @Nested
    class ListUsersTests {
        @Test
        void shouldListExistingUser() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(
                    Collections.singletonList(testUser),
                    pageable,
                    1
            );

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<UserResponse> userResponsePage = userService.listUsers(pageable);
            assertEquals(1, userResponsePage.getTotalElements());
            assertEquals(1, userResponsePage.getContent().size());

            UserResponse userResponse = userResponsePage.getContent().getFirst();
            assertEquals(TEST_USER_ID, userResponse.id());
            assertEquals(TEST_USER_NAME, userResponse.username());
            assertEquals(Role.SUPER_ADMIN.toString(), userResponse.role());
        }

        @Test
        void shouldReturnEmptyPageWhenNoUserFound() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(
                    Collections.emptyList(),
                    pageable,
                    0
            );

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<UserResponse> userResponsePage = userService.listUsers(pageable);
            assertTrue(userPage.isEmpty());
            assertEquals(0, userResponsePage.getTotalElements());
        }
    }

    @Nested
    class DeleteUserTests {
        @Test
        void shouldDeleteUserById() {
            long targetId = 2L;
            User target = new User(
                    "testUser2",
                    TEST_USER_EMAIL,
                    "password",
                    Role.USER
            );
            target.setId(targetId);

            when(userRepository.findById(targetId))
                    .thenReturn(Optional.of(target));

            userService.deleteUser(targetId, testUser);

            verify(userRepository).delete(target);

            ArgumentCaptor<UserDeletedEvent> eventCaptor =
                    ArgumentCaptor.captor();
            verify(publisher).publishEvent(eventCaptor.capture());

            UserDeletedEvent capturedEvent = eventCaptor.getValue();
            assertEquals(target.getEmail(), capturedEvent.email());
            assertEquals(targetId, capturedEvent.id());
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
                    TEST_USER_EMAIL,
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
