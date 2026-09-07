package me.mmtr.userservice;

import me.mmtr.userservice.configuration.UserServiceRabbitMQConfiguration;
import me.mmtr.userservice.model.dto.messaging.UserCreatedEvent;
import me.mmtr.userservice.model.dto.messaging.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.verify;

public class UserNotificationListenerIntegrationTest extends UserServiceModuleIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void setUp() {
        Mockito.reset(rabbitTemplate);
        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    @Test
    void shouldTriggerListenerAfterTransactionCommitWhenUserCreated() {
        UserCreatedEvent userCreatedEvent = new UserCreatedEvent(
                USER_ID, USER_EMAIL
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(userCreatedEvent)
        );

        verify(rabbitTemplate).convertAndSend(
                UserServiceRabbitMQConfiguration.USER_EXCHANGE,
                UserServiceRabbitMQConfiguration.USER_CREATED_KEY,
                userCreatedEvent
        );
    }

    @Test
    void shouldTriggerListenerAfterTransactionCommitWhenUserDeleted() {
        UserDeletedEvent userDeletedEvent = new UserDeletedEvent(
                USER_ID, USER_EMAIL
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(userDeletedEvent)
        );

        verify(rabbitTemplate).convertAndSend(
                UserServiceRabbitMQConfiguration.USER_EXCHANGE,
                UserServiceRabbitMQConfiguration.USER_DELETED_KEY,
                userDeletedEvent
        );
    }

}
