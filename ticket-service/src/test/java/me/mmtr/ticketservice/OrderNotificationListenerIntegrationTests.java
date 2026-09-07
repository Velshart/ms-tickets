package me.mmtr.ticketservice;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
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

public class OrderNotificationListenerIntegrationTests extends TicketServiceModuleIntegrationTest {
    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 1L;
    private static final String EVENT_NAME = "Event";
    private static final long TICKET_QUANTITY = 50;

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
    void shouldTriggerListenerAfterTransactionCommitWhenOrderCreated() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                ORDER_ID,
                USER_ID,
                EVENT_NAME,
                TICKET_QUANTITY
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(event)
        );

        verify(rabbitTemplate).convertAndSend(
                TicketServiceRabbitMQConfiguration.ORDER_EXCHANGE,
                TicketServiceRabbitMQConfiguration.ORDER_CREATED_KEY,
                event
        );
    }


    @Test
    void shouldTriggerListenerAfterTransactionCommitWhenOrderDeleted() {
        OrderDeletedEvent event = new OrderDeletedEvent(
                ORDER_ID,
                USER_ID,
                EVENT_NAME,
                TICKET_QUANTITY
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(event)
        );

        verify(rabbitTemplate).convertAndSend(
                TicketServiceRabbitMQConfiguration.ORDER_EXCHANGE,
                TicketServiceRabbitMQConfiguration.ORDER_DELETED_KEY,
                event
        );
    }
}
