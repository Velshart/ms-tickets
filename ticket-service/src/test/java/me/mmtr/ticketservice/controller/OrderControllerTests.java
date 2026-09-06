package me.mmtr.ticketservice.controller;

import me.mmtr.common.jwt.JwtParser;
import me.mmtr.ticketservice.service.OrderService;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.model.dto.ticket.OrderResponse;
import me.mmtr.ticketservice.model.dto.ticket.TicketResponse;
import me.mmtr.ticketservice.model.exception.OrderNotFoundException;
import me.mmtr.ticketservice.model.exception.TicketsSoldOutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
public class OrderControllerTests {

    private static final Long ORDER_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final int TICKET_QUANTITY = 2;

    @MockitoBean
    private OrderService orderService;
    @MockitoBean
    private JwtParser parser;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    private OrderResponse orderResponse;
    private BuyTicketsRequest buyTicketsRequest;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        user = new AuthenticatedUser(
                USER_ID,
                "user",
                "user@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        orderResponse = new OrderResponse(
                ORDER_ID,
                USER_ID,
                EVENT_ID,
                List.of(new TicketResponse(UUID.randomUUID()))
        );

        buyTicketsRequest = new BuyTicketsRequest(
                EVENT_ID,
                TICKET_QUANTITY
        );
    }


    @Nested
    class GetAllOrdersForUserTests {

        @Test
        void shouldReturnAllOrdersForUser() throws Exception {
            when(orderService.getAllOrdersByUserId(USER_ID))
                    .thenReturn(List.of(orderResponse));

            mockMvc.perform(get("/api/orders")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()")
                            .value(1))
                    .andExpect(jsonPath("$[0].id").value(ORDER_ID));

            verify(orderService).getAllOrdersByUserId(USER_ID);
        }
    }

    @Nested
    class CreateOrderTests {

        @Test
        void shouldCreateOrder() throws Exception {
            when(orderService.createOrder(any(BuyTicketsRequest.class), eq(USER_ID)))
                    .thenReturn(orderResponse);

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ORDER_ID));

            verify(orderService).createOrder(any(BuyTicketsRequest.class), eq(USER_ID));
        }

        @Test
        void shouldReturnConflictWhenTicketsSoldOut() throws Exception {
            when(orderService.createOrder(any(BuyTicketsRequest.class), eq(USER_ID)))
                    .thenThrow(new TicketsSoldOutException(EVENT_ID));

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                    .andExpect(status().isConflict());

            verify(orderService).createOrder(
                    any(BuyTicketsRequest.class),
                    eq(USER_ID)
            );
        }
    }

    @Nested
    class DeleteOrderAsUserTests {

        @Test
        void shouldDeleteOrderAsUser() throws Exception {
            mockMvc.perform(delete("/api/orders/{orderId}", ORDER_ID)
                            .with(csrf())
                            .with(user(user)))
                    .andExpect(status().isNoContent());

            verify(orderService).deleteOrderAsUser(ORDER_ID, USER_ID);
        }

        @Test
        void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
            doThrow(new OrderNotFoundException(ORDER_ID))
                    .when(orderService).deleteOrderAsUser(ORDER_ID, USER_ID);

            mockMvc.perform(delete("/api/orders/{orderId}", ORDER_ID)
                            .with(csrf())
                            .with(user(user)))
                    .andExpect(status().isNotFound());

            verify(orderService).deleteOrderAsUser(ORDER_ID, USER_ID);
        }
    }

    @Nested
    class DeleteOrderAsAdminTests {

        @Test
        void shouldDeleteOrderAsAdmin() throws Exception {
            mockMvc.perform(delete("/api/orders/admin/{orderId}", ORDER_ID)
                            .with(user(user))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(orderService).deleteOrderAsAdmin(ORDER_ID);
        }

        @Test
        void shouldReturnNotFoundWhenOrderDoesNotExistForAdmin() throws Exception {
            doThrow(new OrderNotFoundException(ORDER_ID))
                    .when(orderService).deleteOrderAsAdmin(ORDER_ID);

            mockMvc.perform(delete("/api/orders/admin/{orderId}", ORDER_ID)
                            .with(csrf())
                            .with(user(user)))
                    .andExpect(status().isNotFound());

            verify(orderService).deleteOrderAsAdmin(ORDER_ID);
        }
    }


    @Nested
    class GetOrderByIdTests {

        @Test
        void shouldReturnOrderById() throws Exception {
            when(orderService.getOrder(ORDER_ID, USER_ID)).thenReturn(orderResponse);

            mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID)
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(ORDER_ID))
                    .andExpect(jsonPath("$.eventId").value(EVENT_ID));

            verify(orderService).getOrder(ORDER_ID, USER_ID);
        }

        @Test
        void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
            when(orderService.getOrder(ORDER_ID, USER_ID))
                    .thenThrow(new OrderNotFoundException(ORDER_ID));

            mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID)
                            .with(user(user)))
                    .andExpect(status().isNotFound());

            verify(orderService).getOrder(ORDER_ID, USER_ID);
        }
    }
}