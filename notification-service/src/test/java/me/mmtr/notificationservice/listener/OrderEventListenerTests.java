package me.mmtr.notificationservice.listener;

import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.model.dto.message.OrderCreatedMessage;
import me.mmtr.notificationservice.model.dto.message.OrderDeletedMessage;
import me.mmtr.notificationservice.service.MailService;
import me.mmtr.notificationservice.service.UserIdEmailService;
import me.mmtr.notificationservice.service.listener.OrderEventListener;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderEventListenerTests {

    @Mock
    private MailService mailService;

    @Mock
    private UserIdEmailService userIdEmailService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "user@example.com";

    @Nested
    class HandleOrderCreatedTests {

        @Test
        void shouldHandleOrderCreated() {
            OrderCreatedMessage message = new OrderCreatedMessage(
                    100L,
                    USER_ID,
                    "Concert",
                    2
            );
            UserIdEmailDto userDto = new UserIdEmailDto(USER_ID, USER_EMAIL);

            when(userIdEmailService.findByUserId(USER_ID)).thenReturn(userDto);

            orderEventListener.handleOrderCreated(message);

            verify(userIdEmailService).findByUserId(USER_ID);
            verify(mailService).sendOrderCreatedMail(USER_EMAIL, message);
        }
    }

    @Nested
    class HandleOrderDeletedTests {

        @Test
        void shouldHandleOrderDeleted() {
            OrderDeletedMessage message = new OrderDeletedMessage(
                    100L,
                    USER_ID,
                    "Concert",
                    2
            );
            UserIdEmailDto userDto = new UserIdEmailDto(USER_ID, USER_EMAIL);

            when(userIdEmailService.findByUserId(USER_ID)).thenReturn(userDto);

            orderEventListener.handleOrderDeleted(message);

            verify(userIdEmailService).findByUserId(USER_ID);
            verify(mailService).sendOrderDeletedMail(USER_EMAIL, message);
        }
    }
}