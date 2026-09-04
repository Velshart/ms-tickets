package me.mmtr.notificationservice.listener;

import me.mmtr.notificationservice.model.dto.message.UserCreatedMessage;
import me.mmtr.notificationservice.model.dto.message.UserDeletedMessage;
import me.mmtr.notificationservice.service.MailService;
import me.mmtr.notificationservice.service.UserIdEmailService;
import me.mmtr.notificationservice.service.listener.UserEventListener;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserEventListenerTests {

    @Mock
    private MailService mailService;

    @Mock
    private UserIdEmailService userIdEmailService;

    @InjectMocks
    private UserEventListener userEventListener;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "user@example.com";

    @Nested
    class HandleUserCreatedTests {

        @Test
        void shouldHandleUserCreated() {
            UserCreatedMessage message = new UserCreatedMessage(USER_ID, USER_EMAIL);

            userEventListener.handleUserCreated(message);

            verify(userIdEmailService).create(USER_ID, USER_EMAIL);
            verify(mailService).sendUserCreatedMail(USER_EMAIL);
        }
    }

    @Nested
    class HandleUserDeletedTests {

        @Test
        void shouldHandleUserDeleted() {
            UserDeletedMessage message = new UserDeletedMessage(USER_ID, USER_EMAIL);

            userEventListener.handleUserDeleted(message);

            verify(userIdEmailService).deleteByUserId(USER_ID);
            verify(mailService).sendUserDeletedMail(USER_EMAIL);
        }
    }
}