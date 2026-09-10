package me.mmtr.userservice;

import me.mmtr.common.configuration.CommonRabbitMQConfiguration;
import me.mmtr.common.web.SystemExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({CommonRabbitMQConfiguration.class, SystemExceptionHandler.class})
public class UserServiceApplication {
    static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
