package me.mmtr.ticketservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {
    private static final String QUEUE_NAME = "ticket-requests-queue";
    private static final String EXCHANGE_NAME = "ticket-exchange";
    private static final String ROUTING_KEY = "ticket.request";

    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue ticketRequestsQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding ticketRequestsBinding() {
        return BindingBuilder
                .bind(ticketRequestsQueue())
                .to(ticketExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
