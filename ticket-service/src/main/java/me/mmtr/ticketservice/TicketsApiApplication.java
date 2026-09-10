package me.mmtr.ticketservice;

import me.mmtr.common.configuration.CommonRabbitMQConfiguration;
import me.mmtr.common.web.BaseExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({CommonRabbitMQConfiguration.class, BaseExceptionHandler.class})
public class TicketsApiApplication {
    static void main(String[] args) {
        SpringApplication.run(TicketsApiApplication.class, args);
    }
    //TODO: optimistic lock exception handling
}
