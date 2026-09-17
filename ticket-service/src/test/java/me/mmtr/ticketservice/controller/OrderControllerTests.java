package me.mmtr.ticketservice.controller;

import me.mmtr.ticketservice.assembler.OrderModelAssembler;
import me.mmtr.ticketservice.configuration.filter.RateLimitingFilter;
import me.mmtr.ticketservice.model.OrderStatus;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import me.mmtr.ticketservice.model.dto.order.OrderResponse;
import me.mmtr.ticketservice.model.dto.order.OrderStatusResponse;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.model.exception.OrderNotFoundException;
import me.mmtr.ticketservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitingFilter.class
        ))
@Import(OrderModelAssembler.class)
public class OrderControllerTests {

    private static final Long ORDER_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final int TICKET_QUANTITY = 2;

    @MockitoBean
    private OrderService orderService;

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
                OrderStatus.PENDING,
                List.of()
        );

        buyTicketsRequest = new BuyTicketsRequest(
                EVENT_ID,
                TICKET_QUANTITY
        );
    }

    @Nested
    class GetAllOrdersForUserTests {

        @Test
        void shouldReturnPagedOrdersForUser() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), pageable, 1);

            when(orderService.getAllOrdersByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/orders")
                            .param("page", "0")
                            .param("size", "10")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.orders[0].id").value(ORDER_ID))
                    .andExpect(jsonPath("$._embedded.orders[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$.page.totalElements").value(1));

            verify(orderService).getAllOrdersByUserId(eq(USER_ID), any(Pageable.class));
        }
    }

    @Nested
    class CreateOrderTests {

        @Test
        void shouldSubmitOrderAndReturnAcceptedWithLocationHeader() throws Exception {
            when(orderService.submitOrder(buyTicketsRequest, USER_ID))
                    .thenReturn(orderResponse);

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .with(user(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buyTicketsRequest)))
                    .andExpect(status().isAccepted())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").value(ORDER_ID))
                    .andExpect(jsonPath("$.userId").value(USER_ID))
                    .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                    .andExpect(jsonPath("$.orderStatus").value(OrderStatus.PENDING.toString()))
                    .andExpect(jsonPath("$._links.self.href").exists());
        }
    }

    @Nested
    class GetOrderStatusTests {

        @Test
        void shouldReturnOrderStatusWithOk() throws Exception {
            OrderStatusResponse statusResponse = new OrderStatusResponse(ORDER_ID, OrderStatus.PENDING);

            when(orderService.getOrderStatus(ORDER_ID, USER_ID))
                    .thenReturn(statusResponse);

            mockMvc.perform(get("/api/orders/" + ORDER_ID + "/status")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                    .andExpect(jsonPath("$.orderStatus").value(OrderStatus.PENDING.toString()));
        }

        @Test
        void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
            doThrow(OrderNotFoundException.class)
                    .when(orderService)
                    .getOrderStatus(ORDER_ID, USER_ID);

            mockMvc.perform(get("/api/orders/" + ORDER_ID + "/status")
                            .with(user(user)))
                    .andExpect(status().isNotFound());
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
    class GetUserOrdersTests {
        @Test
        void shouldReturnPagedOrdersForSpecificUserWhenAdmin() throws Exception {
            Long targetUserId = 200L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), pageable, 1);

            when(orderService.getAllOrdersByUserId(eq(targetUserId), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/orders/admin/{userId}", targetUserId)
                            .param("page", "0")
                            .param("size", "10")
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.orders[0].id").value(ORDER_ID))
                    .andExpect(jsonPath("$._embedded.orders[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$.page.totalElements").value(1));

            verify(orderService).getAllOrdersByUserId(eq(targetUserId), any(Pageable.class));
        }
    }

    @Nested
    class GetOrderByIdTests {

        @Test
        void shouldReturnOrderByIdWithHateoasLinks() throws Exception {
            when(orderService.getOrder(ORDER_ID, USER_ID))
                    .thenReturn(orderResponse);

            mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID)
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ORDER_ID))
                    .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.event.href").exists());

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