package me.mmtr.notificationservice;

import me.mmtr.notificationservice.exception.UserEmailNotFoundException;
import me.mmtr.notificationservice.model.UserIdEmail;
import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.repository.UserIdEmailRepository;
import me.mmtr.notificationservice.service.UserIdEmailService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserIdEmailServiceTests {

    @Mock
    private UserIdEmailRepository userIdEmailRepository;

    @InjectMocks
    private UserIdEmailService userIdEmailService;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";

    @Nested
    class CreateTests {
        @Test
        void shouldCreateUserIdEmail() {
            userIdEmailService.create(USER_ID, USER_EMAIL);

            ArgumentCaptor<UserIdEmail> captor = ArgumentCaptor.captor();
            verify(userIdEmailRepository).save(captor.capture());

            UserIdEmail capturedEntity = captor.getValue();
            assertEquals(USER_ID, capturedEntity.getUserId());
            assertEquals(USER_EMAIL, capturedEntity.getEmail());
        }
    }

    @Nested
    class FindByUserIdTests {
        @Test
        void shouldReturnDtoWhenUserFound() {
            UserIdEmail entity = new UserIdEmail(USER_ID, USER_EMAIL);
            when(userIdEmailRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(entity));

            UserIdEmailDto result =
                    userIdEmailService.findByUserId(USER_ID);

            assertNotNull(result);
            assertEquals(USER_ID, result.userId());
            assertEquals(USER_EMAIL, result.email());
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userIdEmailRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(
                    UserEmailNotFoundException.class,
                    () -> userIdEmailService.findByUserId(USER_ID)
            );
        }
    }

    @Nested
    class FindAllByUserIdInTests {
        @Test
        void shouldReturnListOfDtos() {
            List<Long> userIds = List.of(1L, 2L);
            List<UserIdEmail> entities = List.of(
                    new UserIdEmail(1L, "one@example.com"),
                    new UserIdEmail(2L, "two@example.com")
            );
            when(userIdEmailRepository.findAllByUserIdIn(userIds))
                    .thenReturn(entities);

            List<UserIdEmailDto> result =
                    userIdEmailService.findAllByUserIdIn(userIds);

            assertEquals(2, result.size());
            assertEquals(
                    "one@example.com",
                    result.get(0).email()
            );
            assertEquals(
                    "two@example.com",
                    result.get(1).email()
            );
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void shouldDeleteByUserId() {
            userIdEmailService.deleteByUserId((USER_ID));

            verify(userIdEmailRepository).deleteByUserId(USER_ID);
        }
    }
}
