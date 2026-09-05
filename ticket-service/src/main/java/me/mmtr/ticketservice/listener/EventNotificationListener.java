package me.mmtr.ticketservice.listener;

import me.mmtr.ticketservice.configuration.TicketServiceRabbitMQConfiguration;
import me.mmtr.ticketservice.model.dto.messaging.EventDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventStatusChangedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventUpdatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventNotificationListener {
    private final RabbitTemplate rabbitTemplate;

    public EventNotificationListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventStatusChange(EventStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(
                TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                TicketServiceRabbitMQConfiguration.EVENT_STATUS_CHANGED_KEY,
                event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventUpdate(EventUpdatedEvent event) {
        rabbitTemplate.convertAndSend(
                TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                TicketServiceRabbitMQConfiguration.EVENT_UPDATED_KEY,
                event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventDelete(EventDeletedEvent event) {
        rabbitTemplate.convertAndSend(
                TicketServiceRabbitMQConfiguration.EVENT_EXCHANGE,
                TicketServiceRabbitMQConfiguration.EVENT_DELETED_KEY,
                event
        );
    }
}
