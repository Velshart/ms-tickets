package me.mmtr.notificationservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationServiceRabbitMQConfiguration {
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String EVENT_EXCHANGE = "event.exchange";

    public static final String USER_CREATED_QUEUE_NAME = "user.created.queue";
    public static final String USER_DELETED_QUEUE_NAME = "user.deleted.queue";
    private static final String ORDER_CREATED_QUEUE_NAME = "order.created.queue";
    private static final String ORDER_DELETED_QUEUE_NAME = "order.deleted.queue";
    private static final String EVENT_STATUS_CHANGED_QUEUE_NAME = "event.status.changed.queue";
    private static final String EVENT_DELETED_QUEUE_NAME = "event.deleted.queue";
    private static final String EVENT_UPDATED_QUEUE_NAME = "event.updated.queue";


    public static final String USER_CREATED_KEY = "user.created";
    public static final String USER_DELETED_KEY = "user.deleted";
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_DELETED_KEY = "order.deleted";
    public static final String EVENT_STATUS_CHANGED_KEY = "event.status.changed";
    public static final String EVENT_DELETED_KEY = "event.deleted";
    public static final String EVENT_UPDATED_KEY = "event.updated";


    @Bean
    public TopicExchange nsUserExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public Queue nsUserCreatedQueue() {
        return new Queue(USER_CREATED_QUEUE_NAME, true);
    }

    @Bean
    public Queue nsUserDeletedQueue() {
        return new Queue(USER_DELETED_QUEUE_NAME, true);
    }

    @Bean
    public Binding nsUserCreatedBinding() {
        return BindingBuilder
                .bind(nsUserCreatedQueue())
                .to(nsUserExchange())
                .with(USER_CREATED_KEY);
    }

    @Bean
    public Binding nsUserDeletedBinding() {
        return BindingBuilder
                .bind(nsUserDeletedQueue())
                .to(nsUserExchange())
                .with(USER_DELETED_KEY);
    }

    @Bean
    public TopicExchange nsOrderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange nsEventExchange() {
        return new TopicExchange(EVENT_EXCHANGE);
    }

    @Bean
    public Queue nsOrderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE_NAME, true);
    }

    @Bean
    public Queue nsOrderDeletedQueue() {
        return new Queue(ORDER_DELETED_QUEUE_NAME, true);
    }

    @Bean
    public Queue nsEventStatusChangedQueue() {
        return new Queue(EVENT_STATUS_CHANGED_QUEUE_NAME, true);
    }

    @Bean
    public Queue nsEventUpdatedQueue() {
        return new Queue(EVENT_UPDATED_QUEUE_NAME, true);
    }

    @Bean
    public Queue nsEventDeletedQueue() {
        return new Queue(EVENT_DELETED_QUEUE_NAME, true);
    }

    @Bean
    public Binding nsOrderCreatedBinding() {
        return BindingBuilder
                .bind(nsOrderCreatedQueue())
                .to(nsOrderExchange())
                .with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding nsOrderDeletedBinding() {
        return BindingBuilder.bind(nsOrderDeletedQueue())
                .to(nsOrderExchange())
                .with(ORDER_DELETED_KEY);
    }

    @Bean
    public Binding nsEventStatusChangedBinding() {
        return BindingBuilder.bind(nsEventStatusChangedQueue())
                .to(nsEventExchange())
                .with(EVENT_STATUS_CHANGED_KEY);
    }

    @Bean
    public Binding nsEventUpdatedBinding() {
        return BindingBuilder.bind(nsEventUpdatedQueue())
                .to(nsEventExchange())
                .with(EVENT_UPDATED_KEY);
    }

    @Bean
    public Binding nsEventDeletedBinding() {
        return BindingBuilder.bind(nsEventDeletedQueue())
                .to(nsEventExchange())
                .with(EVENT_DELETED_KEY);
    }
}
