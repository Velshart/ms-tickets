package me.mmtr.ticketservice.listener;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderRequestMessage;
import me.mmtr.ticketservice.model.dto.messaging.OrderSubmittedEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderNotificationListenerTests {

    private static final Long ORDER_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final String EVENT_NAME = "Event";
    private static final long TICKET_QUANTITY = 2L;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private ObjectMapper objectMapper;

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
                    TICKET_QUANTITY
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
                    TICKET_QUANTITY
            );

            orderNotificationListener.onOrderDeleted(event);

            verify(rabbitTemplate).convertAndSend(
                    TicketServiceRabbitMQConfiguration.ORDER_EXCHANGE,
                    TicketServiceRabbitMQConfiguration.ORDER_DELETED_KEY,
                    event
            );
        }
    }

    @Nested
    class OrderSubmittedNotificationTests {
        private static final String STREAM_KEY = "order-requests";
        private static final Long EVENT_ID = 1L;

        @Test
        void shouldSendOrderSubmittedMessageToRedis() {
            when(redisTemplate.opsForStream()).thenReturn(streamOperations);

            String serializedMessage = "serialized_message";

            OrderSubmittedEvent event = new OrderSubmittedEvent(
                    ORDER_ID,
                    USER_ID,
                    EVENT_ID,
                    TICKET_QUANTITY
            );

            when(objectMapper.writeValueAsString(any(OrderRequestMessage.class)))
                    .thenReturn(serializedMessage);

            ArgumentCaptor<OrderRequestMessage> messageCaptor =
                    ArgumentCaptor.captor();

            orderNotificationListener.onOrderSubmitted(event);

            verify(objectMapper).writeValueAsString(messageCaptor.capture());
            OrderRequestMessage message = messageCaptor.getValue();
            assertEquals(ORDER_ID, message.orderId());
            assertEquals(USER_ID, message.userId());
            assertEquals(EVENT_ID, message.eventId());
            assertEquals(TICKET_QUANTITY, message.ticketQuantity());

            verify(redisTemplate.opsForStream()).add(
                    STREAM_KEY, Map.of("payload", serializedMessage)
            );
        }
    }
}