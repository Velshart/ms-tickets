package me.mmtr.userservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserServiceRabbitMQConfiguration {
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String USER_DELETED_QUEUE = "user.deleted.queue";

    public static final String USER_CREATED_KEY = "user.created";
    public static final String USER_DELETED_KEY = "user.deleted";

    @Bean
    public TopicExchange usUserExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public Queue usUserCreatedQueue() {
        return new Queue(USER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue usUserDeletedQueue() {
        return new Queue(USER_DELETED_QUEUE, true);
    }

    @Bean
    public Binding usUserCreatedBinding() {
        return BindingBuilder
                .bind(usUserCreatedQueue())
                .to(usUserExchange())
                .with(USER_CREATED_KEY);
    }

    @Bean
    public Binding usUserDeletedBinding() {
        return BindingBuilder
                .bind(usUserDeletedQueue())
                .to(usUserExchange())
                .with(USER_DELETED_KEY);
    }
}
