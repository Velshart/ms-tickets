package me.mmtr.ticketservice;

import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.OrderStatus;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateEventRequest;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.dto.order.OrderResponse;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.repository.OrderRepository;
import me.mmtr.ticketservice.service.EventManagementService;
import me.mmtr.ticketservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderControllerIntegrationTests extends TicketServiceModuleIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EventManagementService eventManagementService;

    @BeforeEach
    public void setup() {
        rateLimitingFilter.reset();
        orderRepository.deleteAll();
    }

    private EventResponse createEventAndOpenTicketSales(String eventName) {
        EventResponse eventResponse =
                eventManagementService.createEvent(new CreateEventRequest(
                        eventName,
                        BigDecimal.valueOf(10),
                        OffsetDateTime.now().plusDays(1),
                        OffsetDateTime.now().plusDays(2),
                        400L
                ));

        eventManagementService.changeEventStatus(
                new ChangeEventStatusRequest(EventStatus.ACTIVE),
                eventResponse.id()
        );
        return eventResponse;
    }

    private OrderResponse createOrder(Long eventId, Long userId) {
        int ticketQuantity = 10;
        return orderService.submitOrder(
                new BuyTicketsRequest(eventId, ticketQuantity), userId
        );
    }

    @Nested
    class GetOrderByIdTests {
        private static final Long USER_ID = 100L;
        private static final String USER_ROLE = "ROLE_USER";

        @Test
        void shouldGetOrderByIdAndReturnOk() throws Exception {
            EventResponse event = createEventAndOpenTicketSales("Event");
            OrderResponse createdOrder = createOrder(event.id(), USER_ID);

            mockMvc.perform(get("/api/orders/{orderId}", createdOrder.id())
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(createdOrder.id()))
                    .andExpect(jsonPath("$.eventId").value(event.id()))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.event.href").exists());
        }

        @Test
        void shouldReturn404WhenOrderNotFound() throws Exception {
            mockMvc.perform(get("/api/orders/{orderId}", 999L)
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetAllOrdersForUserTests {
        private static final Long USER_ID = 100L;
        private static final String USER_ROLE = "ROLE_USER";

        @Test
        void shouldGetAllOrdersForAuthenticatedUserAndReturnOk() throws Exception {
            EventResponse event1 = createEventAndOpenTicketSales("Event 1");
            EventResponse event2 = createEventAndOpenTicketSales("Event 2");

            OrderResponse order1 = createOrder(event1.id(), USER_ID);
            OrderResponse order2 = createOrder(event2.id(), USER_ID);

            mockMvc.perform(get("/api/orders")
                            .param("page", "0")
                            .param("size", "10")
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.orders").isArray())
                    .andExpect(jsonPath("$._embedded.orders.length()").value(2))
                    .andExpect(jsonPath("$._embedded.orders[0].id").value(order1.id()))
                    .andExpect(jsonPath("$._embedded.orders[1].id").value(order2.id()))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$.page.totalElements").value(2));
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoOrders() throws Exception {
            mockMvc.perform(get("/api/orders")
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded").doesNotExist())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }
    }

    @Nested
    class SubmitOrderTests {
        private static final Long USER_ID = 100L;
        private static final String USER_ROLE = "ROLE_USER";

        @Test
        void shouldSubmitOrderAndReturn202Accepted() throws Exception {
            EventResponse event = createEventAndOpenTicketSales("Event");
            BuyTicketsRequest request = new BuyTicketsRequest(
                    event.id(),
                    3
            );

            MvcResult mvcResult = mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .with(withUser(USER_ID, USER_ROLE))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andReturn();

            OrderResponse response = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(),
                    OrderResponse.class
            );

            OrderResponse fetchedOrder = orderService.getOrder(response.id(), USER_ID);
            assertEquals(response.id(), fetchedOrder.id());
            assertEquals(event.id(), fetchedOrder.eventId());
            assertEquals(OrderStatus.PENDING, fetchedOrder.orderStatus());

            Page<OrderResponse> userOrders = orderService.getAllOrdersByUserId(USER_ID, PageRequest.of(0, 10));
            assertEquals(1, userOrders.getTotalElements());
            assertEquals(response.id(), userOrders.getContent().getFirst().id());
        }

        @Test
        void shouldReturn404WhenOrderingForNonExistingEvent() throws Exception {
            BuyTicketsRequest request = new BuyTicketsRequest(999L, 2);

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .with(withUser(USER_ID, USER_ROLE))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteOrderAsUserTests {
        private static final Long USER_ID = 100L;
        private static final String USER_ROLE = "ROLE_USER";

        @Test
        void shouldDeleteOwnOrderAndReturnNoContent() throws Exception {
            EventResponse event = createEventAndOpenTicketSales("Event");
            OrderResponse createdOrder = createOrder(event.id(), USER_ID);

            mockMvc.perform(delete("/api/orders/{orderId}", createdOrder.id())
                            .with(csrf())
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isNoContent());

            Page<OrderResponse> userOrders = orderService.getAllOrdersByUserId(
                    USER_ID,
                    PageRequest.of(0, 10)
            );
            assertTrue(userOrders.isEmpty());
        }

        @Test
        void shouldReturnNotFoundWhenDeletingNonExistingOrder() throws Exception {
            mockMvc.perform(delete("/api/orders/{orderId}", 999L)
                            .with(csrf())
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteOrderAsAdminTests {
        private static final Long USER_ID = 100L;
        private static final Long ADMIN_ID = 1L;

        @Test
        void shouldDeleteOrderAsAdminAndReturnNoContent() throws Exception {
            EventResponse event = createEventAndOpenTicketSales("Event");
            OrderResponse createdOrder = createOrder(event.id(), USER_ID);

            mockMvc.perform(delete("/api/orders/admin/{orderId}", createdOrder.id())
                            .with(csrf())
                            .with(withUser(ADMIN_ID, "ROLE_ADMIN")))
                    .andExpect(status().isNoContent());

            Page<OrderResponse> userOrders = orderService.getAllOrdersByUserId(USER_ID, PageRequest.of(0, 10));
            assertTrue(userOrders.isEmpty());
        }

        @Test
        void shouldReturnForbiddenWhenRegularUserTriesToDeleteOrderAsAdmin() throws Exception {
            EventResponse event = createEventAndOpenTicketSales("Event");
            OrderResponse createdOrder = createOrder(event.id(), USER_ID);

            mockMvc.perform(delete("/api/orders/admin/{orderId}", createdOrder.id())
                            .with(csrf())
                            .with(withUser(USER_ID, "ROLE_USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnNotFoundWhenAdminDeletesNonExistingOrder() throws Exception {
            mockMvc.perform(delete("/api/orders/admin/{orderId}", 999L)
                            .with(csrf())
                            .with(withUser(ADMIN_ID, "ROLE_ADMIN")))
                    .andExpect(status().isNotFound());
        }
    }
}