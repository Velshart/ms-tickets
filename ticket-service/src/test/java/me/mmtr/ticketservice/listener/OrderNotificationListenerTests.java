package me.mmtr.ticketservice.listener;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OrderNotificationListenerTests {

    private static final Long ORDER_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final String EVENT_NAME = "Event";
    private static final long TICKETS_QUANTITY = 2L;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderNotificationListener orderNotificationListener;

    @Nested
    class OrderCreatedNotificationTests {

        @Test
        void shouldSendOrderCreatedMessageToRabbitMQ() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    ORDER_ID,
                    USER_ID,
                    EVENT_NAME,
                    TICKETS_QUANTITY
            );

            orderNotificationListener.onOrderCreated(event);

            verify(rabbitTemplate).convertAndSend(
                    TicketServiceRabbitMQConfiguration.ORDER_EXCHANGE,
                    TicketServiceRabbitMQConfiguration.ORDER_CREATED_KEY,
                    event
            );
        }
    }

    @Nested
    class OrderDeletedNotificationTests {

        @Test
        void shouldSendOrderDeletedMessageToRabbitMQ() {
            OrderDeletedEvent event = new OrderDeletedEvent(
                    ORDER_ID,
                    USER_ID,
                    EVENT_NAME,
                    TICKETS_QUANTITY
            );

            orderNotificationListener.onOrderDeleted(event);

            verify(rabbitTemplate).convertAndSend(
                    TicketServiceRabbitMQConfiguration.ORDER_EXCHANGE,
                    TicketServiceRabbitMQConfiguration.ORDER_DELETED_KEY,
                    event
            );
        }
    }
}