package me.mmtr.userservice.service.listener;

import me.mmtr.userservice.configuration.UserServiceRabbitMQConfiguration;
import me.mmtr.userservice.model.dto.messaging.UserCreatedEvent;
import me.mmtr.userservice.model.dto.messaging.UserDeletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserNotificationListener {
    private final RabbitTemplate rabbitTemplate;

    public UserNotificationListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                UserServiceRabbitMQConfiguration.USER_EXCHANGE,
                UserServiceRabbitMQConfiguration.USER_CREATED_KEY,
                event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        rabbitTemplate.convertAndSend(
                UserServiceRabbitMQConfiguration.USER_EXCHANGE,
                UserServiceRabbitMQConfiguration.USER_DELETED_KEY,
                event
        );
    }
}
