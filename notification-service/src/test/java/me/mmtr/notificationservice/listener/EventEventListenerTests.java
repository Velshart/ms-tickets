package me.mmtr.notificationservice.listener;

import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.model.dto.message.EventDeletedMessage;
import me.mmtr.notificationservice.model.dto.message.EventStatusChangedMessage;
import me.mmtr.notificationservice.model.dto.message.EventUpdatedMessage;
import me.mmtr.notificationservice.service.MailService;
import me.mmtr.notificationservice.service.UserIdEmailService;
import me.mmtr.notificationservice.service.listener.EventEventListener;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventEventListenerTests {

    @Mock
    private MailService mailService;

    @Mock
    private UserIdEmailService userIdEmailService;

    @InjectMocks
    private EventEventListener eventEventListener;

    @Nested
    class HandleEventStatusChangedTests {

        @Test
        void shouldHandleEventStatusChanged() {
            List<Long> ownerIds = List.of(1L, 2L);
            EventStatusChangedMessage message =
                    new EventStatusChangedMessage("Rock Concert", ownerIds);
            List<UserIdEmailDto> userDtos = List.of(
                    new UserIdEmailDto(1L, "user1@example.com"),
                    new UserIdEmailDto(2L, "user2@example.com")
            );

            when(userIdEmailService.findAllByUserIdIn(ownerIds))
                    .thenReturn(userDtos);

            eventEventListener.handleEventStatusChanged(message);

            verify(userIdEmailService).findAllByUserIdIn(ownerIds);
            verify(mailService).sendEventStatusChangedMail(message, userDtos);
        }
    }

    @Nested
    class HandleEventUpdatedTests {

        @Test
        void shouldHandleEventUpdated() {
            List<Long> ownerIds = List.of(1L, 2L);
            EventUpdatedMessage message = new EventUpdatedMessage(
                    "Old Name",
                    BigDecimal.valueOf(60.00),
                    OffsetDateTime.now().plusDays(1),
                    OffsetDateTime.now().minusDays(2),
                    100,
                    "New Name",
                    BigDecimal.valueOf(50.00),
                    OffsetDateTime.now().plusDays(3),
                    OffsetDateTime.now().plusDays(4),
                    80,
                    ownerIds
            );
            List<UserIdEmailDto> userDtos = List.of(
                    new UserIdEmailDto(1L, "user1@example.com"),
                    new UserIdEmailDto(2L, "user2@example.com")
            );

            when(userIdEmailService.findAllByUserIdIn(ownerIds))
                    .thenReturn(userDtos);

            eventEventListener.handleEventUpdated(message);

            verify(userIdEmailService).findAllByUserIdIn(ownerIds);
            verify(mailService).sendEventUpdatedMail(message, userDtos);
        }
    }

    @Nested
    class HandleEventDeletedTests {

        @Test
        void shouldHandleEventDeleted() {
            List<Long> ownerIds = List.of(1L, 2L);
            EventDeletedMessage message =
                    new EventDeletedMessage("Event", ownerIds);
            List<UserIdEmailDto> userDtos = List.of(
                    new UserIdEmailDto(1L, "user1@example.com"),
                    new UserIdEmailDto(2L, "user2@example.com")
            );

            when(userIdEmailService.findAllByUserIdIn(ownerIds))
                    .thenReturn(userDtos);

            eventEventListener.handleEventDeleted(message);

            verify(userIdEmailService).findAllByUserIdIn(ownerIds);
            verify(mailService).sendEventDeletedMail(message, userDtos);
        }
    }
}
