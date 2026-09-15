package me.mmtr.ticketservice;

import me.mmtr.common.configuration.CommonRabbitMQConfiguration;
import me.mmtr.common.web.SystemExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@Import({CommonRabbitMQConfiguration.class, SystemExceptionHandler.class})
public class TicketsApiApplication {
    static void main(String[] args) {
        SpringApplication.run(TicketsApiApplication.class, args);
    }
}
