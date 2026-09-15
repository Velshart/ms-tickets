package me.mmtr.servicesintegrationtests;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import me.mmtr.notificationservice.repository.UserIdEmailRepository;
import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.OrderStatus;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateEventRequest;
import me.mmtr.ticketservice.model.dto.event.UpdateEventRequest;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.repository.EventRepository;
import me.mmtr.ticketservice.repository.OrderRepository;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ServicesIntegrationTests {
    @ServiceConnection
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");
    @ServiceConnection
    @SuppressWarnings("resource")
    private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);
    @ServiceConnection
    private static final RabbitMQContainer rabbitmq = new RabbitMQContainer(
            "rabbitmq:3.12-management"
    );
    private static final GreenMail greenMail =
            new GreenMail(ServerSetupTest.SMTP)
                    .withConfiguration(
                            GreenMailConfiguration
                                    .aConfig()
                                    .withDisabledAuthentication()
                    );
    @Value("${super.admin.username}")
    private String superAdminUsername;
    @Value("${super.admin.password}")
    private String superAdminPassword;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserIdEmailRepository userIdEmailRepository;
    private AuthenticatedUser adminUser;

    @DynamicPropertySource
    static void greenMailProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.mail.host",
                () -> greenMail.getSmtp().getBindTo()
        );
        registry.add(
                "spring.mail.port",
                () -> greenMail.getSmtp().getPort()
        );
    }

    @BeforeAll
    public static void startContainers() {
        greenMail.start();
    }

    @AfterAll
    public static void stopContainers() {
        greenMail.stop();
    }

    @BeforeEach
    void setUp() throws FolderException {
        orderRepository.deleteAll();
        eventRepository.deleteAll();
        userIdEmailRepository.deleteAll();
        userRepository.deleteAll();
        greenMail.purgeEmailFromAllMailboxes();

        adminUser = new AuthenticatedUser(
                1L,
                superAdminUsername,
                superAdminPassword,
                List.of(new SimpleGrantedAuthority("ROLE_" + Role.SUPER_ADMIN))
        );
    }

    @Test
    void shouldAllowToBuyTicketsAndSendEmail() throws Exception {
        CreateEventRequest createEvent = new CreateEventRequest(
                "Event",
                BigDecimal.valueOf(120.00),
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                100L
        );

        MvcResult eventResult = mockMvc.perform(post("/api/events")
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEvent)))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper
                .readTree(eventResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        ChangeEventStatusRequest changeEventStatusRequest = new ChangeEventStatusRequest(
                EventStatus.ACTIVE
        );
        mockMvc.perform(patch("/api/events/" + eventId + "/status")
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(changeEventStatusRequest)))
                .andExpect(status().isOk());

        RegisterRequest registerUserRequest = new RegisterRequest(
                "test_user",
                "test@example.com",
                "password"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long userId = objectMapper
                .readTree(registerResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        AuthenticatedUser normalUser = new AuthenticatedUser(
                userId,
                registerUserRequest.username(),
                registerUserRequest.email(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        BuyTicketsRequest orderRequest =
                new BuyTicketsRequest(eventId, 2);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .with(user(normalUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isAccepted());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedEvent = eventRepository.findById(eventId).orElseThrow();
            assertEquals(98, updatedEvent.getAvailableTickets());
            assertEquals(1, orderRepository.findAll().size());
        });

        boolean emailArrived =
                greenMail.waitForIncomingEmail(5000, 2);
        assertTrue(emailArrived);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(2, receivedMessages.length);
        assertEquals(
                normalUser.email(),
                receivedMessages[0].getAllRecipients()[0].toString()
        );
    }

    @Test
    void shouldSendMailToAllTicketOwnersAfterEventWasUpdated() throws Exception {
        CreateEventRequest createEventRequest = new CreateEventRequest(
                "Event",
                BigDecimal.valueOf(50.00),
                OffsetDateTime.now().plusDays(5),
                OffsetDateTime.now().plusDays(6),
                50L
        );

        MvcResult createEventResult = mockMvc.perform(post("/api/events")
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long createdEventId = objectMapper
                .readTree(createEventResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        ChangeEventStatusRequest changeEventStatusRequest = new ChangeEventStatusRequest(
                EventStatus.ACTIVE
        );
        mockMvc.perform(patch("/api/events/{id}/status", createdEventId)
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeEventStatusRequest)))
                .andExpect(status().isOk());

        String userAUsername = "userA";
        String userAEmail = "usera@example.com";
        String userBUsername = "userB";
        String userBEmail = "userb@example.com";
        Long userAId = registerUser(userAUsername, userAEmail);
        Long userBId = registerUser(userBUsername, userBEmail);

        AuthenticatedUser userA = new AuthenticatedUser(
                userAId,
                userAUsername,
                userAEmail,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        AuthenticatedUser userB = new AuthenticatedUser(
                userBId,
                userBUsername,
                userBEmail,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        BuyTicketsRequest buyTicketsRequest = new BuyTicketsRequest(
                createdEventId, 5
        );

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .with(user(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .with(user(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                .andExpect(status().isAccepted());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(2, orderRepository.findAll().size());
            Event event = eventRepository.findById(createdEventId).orElseThrow();
            assertEquals(40, event.getAvailableTickets());
        });

        greenMail.purgeEmailFromAllMailboxes();

        UpdateEventRequest updateEventRequest = new UpdateEventRequest(
                "New Event", BigDecimal.valueOf(50.00),
                OffsetDateTime.now().plusDays(5).plusHours(2),
                OffsetDateTime.now().plusDays(6),
                null
        );

        mockMvc.perform(put("/api/events/{id}", createdEventId)
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateEventRequest)))
                .andExpect(status().isOk());

        boolean emailsArrived = greenMail.waitForIncomingEmail(5000, 2);
        assertTrue(emailsArrived);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(2, receivedMessages.length);
    }

    @Test
    void shouldDeleteOrderAndIncreaseNumberOfAvailableTickets() throws Exception {
        CreateEventRequest createEventRequest = new CreateEventRequest(
                "Event", BigDecimal.valueOf(80.00),
                OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(3).plusHours(2),
                10L
        );

        MvcResult createEventResult = mockMvc.perform(post("/api/events")
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long createdEventId = objectMapper
                .readTree(createEventResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        ChangeEventStatusRequest changeEventStatusRequest =
                new ChangeEventStatusRequest(
                        EventStatus.ACTIVE
                );
        mockMvc.perform(patch("/api/events/{id}/status", createdEventId)
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeEventStatusRequest)))
                .andExpect(status().isOk());

        String username = "test_user";
        String userEmail = "testuser@example.com";
        Long userId = registerUser(username, userEmail);
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                username,
                userEmail,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        BuyTicketsRequest buyTicketsRequest = new BuyTicketsRequest(
                createdEventId, 1
        );

        MvcResult createOrderResult = mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                .andExpect(status().isAccepted())
                .andReturn();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, orderRepository.findAll().size());
            Event event = eventRepository.findById(createdEventId).orElseThrow();
            assertEquals(9L, event.getAvailableTickets());
        });

        Long createdOrderId = objectMapper
                .readTree(createOrderResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(delete("/api/orders/{id}", createdOrderId)
                        .with(csrf())
                        .with(user(user)))
                .andExpect(status().isNoContent());

        assertEquals(
                createEventRequest.availableTickets(),
                eventRepository
                        .findById(createdEventId).orElseThrow().getAvailableTickets()
        );
    }

    @Test
    void shouldSetTicketSoldOutStatusForOrderWhenNoTicketsAvailable() throws Exception {
        CreateEventRequest createEventRequest = new CreateEventRequest(
                "Event", BigDecimal.valueOf(80.00),
                OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(3).plusHours(2),
                1L
        );

        MvcResult createEventResult = mockMvc.perform(post("/api/events")
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long createdEventId = objectMapper
                .readTree(createEventResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        ChangeEventStatusRequest changeEventStatusRequest =
                new ChangeEventStatusRequest(
                        EventStatus.ACTIVE
                );
        mockMvc.perform(patch("/api/events/{id}/status", createdEventId)
                        .with(csrf())
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeEventStatusRequest)))
                .andExpect(status().isOk());

        String userAUsername = "userA";
        String userAEmail = "usera@example.com";
        String userBUsername = "userB";
        String userBEmail = "userb@example.com";
        Long userAId = registerUser(userAUsername, userAEmail);
        Long userBId = registerUser(userBUsername, userBEmail);

        AuthenticatedUser userA = new AuthenticatedUser(
                userAId,
                userAUsername,
                userAEmail,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        AuthenticatedUser userB = new AuthenticatedUser(
                userBId,
                userBUsername,
                userBEmail,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        BuyTicketsRequest buyTicketsRequest = new BuyTicketsRequest(
                createdEventId, 1
        );
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .with(user(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                .andExpect(status().isAccepted());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, orderRepository.findAll().size());
            Event event = eventRepository.findById(createdEventId).orElseThrow();
            assertEquals(0L, event.getAvailableTickets());
        });

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .with(user(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                .andExpect(status().isAccepted())
                .andReturn();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var orders = orderRepository.findAll();
            assertEquals(2, orders.size());

            long confirmedOrdersCount = orders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.CONFIRMED)
                    .count();
            long soldOutOrdersCount = orders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.TICKETS_SOLD_OUT)
                    .count();

            assertEquals(1, confirmedOrdersCount);
            assertEquals(1, soldOutOrdersCount);

            assertEquals(
                    0,
                    eventRepository.findById(createdEventId)
                            .orElseThrow()
                            .getAvailableTickets()
            );
        });
    }

    private Long registerUser(String username, String email) throws Exception {
        String password = "password";
        RegisterRequest registerRequest = new RegisterRequest(
                username,
                email,
                password
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }
}
