package me.mmtr.userservice.service.listener;

import me.mmtr.userservice.configuration.UserServiceRabbitMQConfiguration;
import me.mmtr.userservice.model.dto.messaging.UserCreatedEvent;
import me.mmtr.userservice.model.dto.messaging.UserDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserNotificationListenerTests {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserNotificationListener listener;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";

    @Test
    void shouldSendUserCreatedEventToRabbitMQ() {
        UserCreatedEvent event = new UserCreatedEvent(
                USER_ID,
                USER_EMAIL
        );

        listener.handleUserCreatedEvent(event);

        verify(rabbitTemplate).convertAndSend(
                UserServiceRabbitMQConfiguration.USER_EXCHANGE,
                UserServiceRabbitMQConfiguration.USER_CREATED_KEY,
                event
        );
    }

    @Test
    void shouldSendUserDeletedEventToRabbitMQ() {
        UserDeletedEvent event = new UserDeletedEvent(
                USER_ID,
                USER_EMAIL
        );

        listener.handleUserDeletedEvent(event);

        verify(rabbitTemplate).convertAndSend(
                UserServiceRabbitMQConfiguration.USER_EXCHANGE,
                UserServiceRabbitMQConfiguration.USER_DELETED_KEY,
                event
        );
    }
}
