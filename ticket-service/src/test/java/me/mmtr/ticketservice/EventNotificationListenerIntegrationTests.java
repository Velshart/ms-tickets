package me.mmtr.ticketservice;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.EventDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventStatusChangedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;

public class EventNotificationListenerIntegrationTests extends TicketServiceModuleIntegrationTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    public void setUp() {
        rateLimitingFilter.reset();
        Mockito.reset(rabbitTemplate);
        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    @Test
    void shouldTriggerListenerAfterTransactionCommitWhenEventStatusWasChanged() {
        EventStatusChangedEvent event = new EventStatusChangedEvent(
                "Event",
                List.of(1L, 2L)
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(event));

        verify(rabbitTemplate).convertAndSend(
                TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                TicketServiceRabbitMQConfiguration.EVENT_STATUS_CHANGED_KEY,
                event
        );
    }

    @Test
    void shouldTriggerListenerAfterTransactionCommitWhenEventUpdated() {
        EventUpdatedEvent event = new EventUpdatedEvent(
                "Event",
                BigDecimal.TEN,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                50,
                "New Event",
                BigDecimal.valueOf(20),
                OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(3),
                60,
                List.of(1L, 2L)
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(event));

        verify(rabbitTemplate).convertAndSend(
                TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                TicketServiceRabbitMQConfiguration.EVENT_UPDATED_KEY,
                event
        );
    }

    @Test
    void shouldTriggerListenerAfterTransactionCommitWhenEventDeleted() {
        EventDeletedEvent event = new EventDeletedEvent(
                "Event",
                List.of(1L, 2L)
        );

        transactionTemplate.executeWithoutResult(_ ->
                publisher.publishEvent(event));

        verify(rabbitTemplate).convertAndSend(
                TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                TicketServiceRabbitMQConfiguration.EVENT_DELETED_KEY,
                event
        );
    }
}
