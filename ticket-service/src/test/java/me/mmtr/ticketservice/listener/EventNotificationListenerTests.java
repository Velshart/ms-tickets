package me.mmtr.ticketservice.listener;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.EventDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventStatusChangedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventUpdatedEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EventNotificationListenerTests {

    private static final String EVENT_NAME = "Event";
    private static final List<Long> USER_IDS = List.of(100L, 101L);

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventNotificationListener eventNotificationListener;

    @Nested
    class StatusAndUpdatedNotificationTests {

        @Test
        void shouldSendEventStatusChangedMessageToRabbitMQ() {
            EventStatusChangedEvent event =
                    new EventStatusChangedEvent(EVENT_NAME, USER_IDS);

            eventNotificationListener.onEventStatusChange(event);

            verify(rabbitTemplate).convertAndSend(
                    TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                    TicketServiceRabbitMQConfiguration.EVENT_STATUS_CHANGED_KEY,
                    event
            );
        }

        @Test
        void shouldSendEventUpdatedMessageToRabbitMQ() {
            EventUpdatedEvent event = new EventUpdatedEvent(
                    "Old", BigDecimal.TEN, 100L,
                    "New", BigDecimal.valueOf(20), 150L,
                    USER_IDS
            );

            eventNotificationListener.onEventUpdate(event);

            verify(rabbitTemplate).convertAndSend(
                    TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                    TicketServiceRabbitMQConfiguration.EVENT_UPDATED_KEY,
                    event
            );
        }
    }

    @Nested
    class DeleteNotificationTests {

        @Test
        void shouldSendEventDeletedMessageToRabbitMQ() {
            EventDeletedEvent event = new EventDeletedEvent(EVENT_NAME, USER_IDS);

            eventNotificationListener.onEventDelete(event);

            verify(rabbitTemplate).convertAndSend(
                    TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                    TicketServiceRabbitMQConfiguration.EVENT_DELETED_KEY,
                    event
            );
        }
    }
}