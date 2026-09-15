package me.mmtr.ticketservice.listener;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderRequestMessage;
import me.mmtr.ticketservice.model.dto.messaging.OrderSubmittedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class OrderNotificationListener {
    private static final String STREAM_KEY = "order-requests";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    public OrderNotificationListener(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RabbitTemplate rabbitTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                TicketServiceRabbitMQConfiguration.ORDER_EXCHANGE,
                TicketServiceRabbitMQConfiguration.ORDER_CREATED_KEY,
                event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderDeleted(OrderDeletedEvent event) {
        rabbitTemplate.convertAndSend(
                TicketServiceRabbitMQConfiguration.ORDER_EXCHANGE,
                TicketServiceRabbitMQConfiguration.ORDER_DELETED_KEY,
                event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        OrderRequestMessage message = new OrderRequestMessage(
                event.orderId(),
                event.userId(),
                event.eventId(),
                event.ticketQuantity()
        );
        String payload = objectMapper.writeValueAsString(message);
        redisTemplate
                .opsForStream()
                .add(STREAM_KEY, Map.of("payload", payload));
    }
}
