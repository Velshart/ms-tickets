package me.mmtr.ticketservice.listener;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {
    private final RabbitTemplate rabbitTemplate;

    public OrderNotificationListener(RabbitTemplate rabbitTemplate) {
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
}
