package me.mmtr.ticketservice;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderRequestMessage;
import me.mmtr.ticketservice.model.dto.messaging.OrderSubmittedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderNotificationListenerIntegrationTests extends TicketServiceModuleIntegrationTest {
    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 1L;
    private static final Long EVENT_ID = 1L;
    private static final String EVENT_NAME = "Event";
    private static final long TICKET_QUANTITY = 50;

    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StreamOperations<String, Object, Object> streamOperations;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    public void setUp() {
        rateLimitingFilter.reset();
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

    @Test
    void shouldTriggerListenerAfterTransactionCommitWhenOrderSubmitted() {
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);
        OrderSubmittedEvent event = new OrderSubmittedEvent(
                ORDER_ID,
                USER_ID,
                EVENT_ID,
                TICKET_QUANTITY
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(event)
        );

        OrderRequestMessage message = new OrderRequestMessage(
                event.orderId(),
                event.userId(),
                event.eventId(),
                event.ticketQuantity()
        );

        verify(streamOperations).add(
                "order-requests",
                Map.of(
                        "payload",
                        objectMapper.writeValueAsString(message)
                )
        );
    }
}
