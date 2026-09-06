package me.mmtr.ticketservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketServiceRabbitMQConfiguration {
    private static final String ORDER_CREATED_QUEUE_NAME = "order.created.queue";
    private static final String ORDER_DELETED_QUEUE_NAME = "order.deleted.queue";
    private static final String EVENT_STATUS_CHANGED_QUEUE_NAME = "event.status.changed.queue";
    private static final String EVENT_UPDATED_QUEUE_NAME = "event.updated.queue";
    private static final String EVENT_DELETED_QUEUE_NAME = "event.deleted.queue";

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String EVENT_EXCHANGE = "event.exchange";

    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_DELETED_KEY = "order.deleted";
    public static final String EVENT_STATUS_CHANGED_KEY = "event.status.changed";
    public static final String EVENT_UPDATED_KEY = "event.updated";
    public static final String EVENT_DELETED_KEY = "event.deleted";

    @Bean
    public TopicExchange tsOrderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange tsEventExchange() {
        return new TopicExchange(EVENT_EXCHANGE);
    }

    @Bean
    public Queue tsOrderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE_NAME, true);
    }

    @Bean
    public Queue tsOrderDeletedQueue() {
        return new Queue(ORDER_DELETED_QUEUE_NAME, true);
    }

    @Bean
    public Queue tsEventStatusChangedQueue() {
        return new Queue(EVENT_STATUS_CHANGED_QUEUE_NAME, true);
    }

    @Bean
    public Queue tsEventUpdatedQueue() {
        return new Queue(EVENT_UPDATED_QUEUE_NAME, true);
    }

    @Bean
    public Queue tsEventDeletedQueue() {
        return new Queue(EVENT_DELETED_QUEUE_NAME, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(tsOrderCreatedQueue())
                .to(tsOrderExchange())
                .with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding orderDeletedBinding() {
        return BindingBuilder.bind(tsOrderDeletedQueue())
                .to(tsOrderExchange())
                .with(ORDER_DELETED_KEY);
    }

    @Bean
    public Binding eventStatusChangedBinding() {
        return BindingBuilder.bind(tsEventStatusChangedQueue())
                .to(tsEventExchange())
                .with(EVENT_STATUS_CHANGED_KEY);
    }

    @Bean
    public Binding eventUpdatedBinding() {
        return BindingBuilder.bind(tsEventUpdatedQueue())
                .to(tsEventExchange())
                .with(EVENT_UPDATED_KEY);
    }

    @Bean
    public Binding eventDeletedBinding() {
        return BindingBuilder.bind(tsEventDeletedQueue())
                .to(tsEventExchange())
                .with(EVENT_DELETED_KEY);
    }
}
