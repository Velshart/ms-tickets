package me.mmtr.notificationservice;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import me.mmtr.notificationservice.model.UserIdEmail;
import me.mmtr.notificationservice.model.dto.message.*;
import me.mmtr.notificationservice.repository.UserIdEmailRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class NotificationServiceIntegrationTest {
    @ServiceConnection
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @ServiceConnection
    private static final RabbitMQContainer rabbitmq = new RabbitMQContainer(
            "rabbitmq:3.12-management"
    );

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserIdEmailRepository repository;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    public void setup() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
        repository.deleteAll();
    }

    @Test
    void shouldSendMailAndSaveUserEmailWhenUserCreated() {
        UserCreatedMessage userCreatedMessage = new UserCreatedMessage(
                100L, "test@example.com"
        );

        rabbitTemplate.convertAndSend(
                "user.exchange",
                "user.created",
                userCreatedMessage
        );

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertTrue(repository.findByUserId(100L).isPresent());
            verify(mailSender).send(any(MimeMessage.class));
        });
    }

    @Test
    void shouldDeleteUserEmailWhenUserDeleted() {
        repository.save(new UserIdEmail(100L, "test@example.com"));

        UserDeletedMessage message =
                new UserDeletedMessage(100L, "test@example.com");

        rabbitTemplate.convertAndSend(
                "user.exchange",
                "user.deleted",
                message
        );

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> {
                    assertTrue(repository.findByUserId(100L).isEmpty());
                    verify(mailSender).send(any(MimeMessage.class));
                }
        );
    }

    @Test
    void shouldSendMailWhenOrderCreated() {
        repository.save(new UserIdEmail(100L, "test@example.com"));

        OrderCreatedMessage message = new OrderCreatedMessage(
                1L,
                100L,
                "Event",
                2
        );

        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.created", message
        );

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> verify(mailSender).send(any(MimeMessage.class))
        );
    }

    @Test
    void shouldSendMailWhenOrderDeleted() {
        repository.save(new UserIdEmail(100L, "test@example.com"));

        OrderDeletedMessage message = new OrderDeletedMessage(
                1L,
                100L,
                "Event",
                2
        );

        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.deleted", message
        );

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> verify(mailSender).send(any(MimeMessage.class))
        );
    }

    @Test
    void shouldSendMailWhenEventStatusChanged() {
        repository.save(new UserIdEmail(100L, "test@example.com"));

        EventStatusChangedMessage message = new EventStatusChangedMessage(
                "EVENT", List.of(100L)
        );

        rabbitTemplate.convertAndSend(
                "event.exchange",
                "event.status.changed",
                message
        );

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> verify(mailSender).send(any(MimeMessage.class))
        );
    }

    @Test
    void shouldSendMailWhenEventUpdated() {
        repository.save(new UserIdEmail(100L, "test@example.com"));

        EventUpdatedMessage message = new EventUpdatedMessage(
                "Old Event",
                BigDecimal.valueOf(10.0),
                40,
                "New Event",
                BigDecimal.valueOf(20.0),
                50,
                List.of(100L)
        );

        rabbitTemplate.convertAndSend(
                "event.exchange",
                "event.updated",
                message
        );

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> verify(mailSender).send(any(MimeMessage.class))
        );
    }

    @Test
    void shouldSendMailWhenEventDeleted() {
        repository.save(new UserIdEmail(100L, "test@example.com"));
        EventDeletedMessage message = new EventDeletedMessage(
                "Event",
                List.of(100L)
        );

        rabbitTemplate.convertAndSend(
                "event.exchange",
                "event.deleted",
                message
        );

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> verify(mailSender).send(any(MimeMessage.class))
        );
    }
}
