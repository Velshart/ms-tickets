package me.mmtr.ticketservice.assembler;

import me.mmtr.ticketservice.model.OrderStatus;
import me.mmtr.ticketservice.model.dto.order.OrderResponse;
import me.mmtr.ticketservice.model.dto.ticket.TicketResponse;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrderModelAssemblerTests {
    private final OrderModelAssembler orderModelAssembler = new OrderModelAssembler();

    @Test
    void shouldCreateEntityModelWithCorrectLinks() {
        OrderResponse orderResponse = new OrderResponse(
                1L,
                1L,
                1L,
                OrderStatus.PENDING,
                List.of(new TicketResponse(UUID.randomUUID()))
        );

        EntityModel<OrderResponse> entityModel =
                orderModelAssembler.toModel(orderResponse);

        assertNotNull(entityModel);
        assertEquals(entityModel.getContent(), orderResponse);

        Optional<Link> selfLink = entityModel.getLink("self");
        assertTrue(selfLink.isPresent());
        assertTrue(selfLink.get().getHref().endsWith("/api/orders/1"));

        Optional<Link> userOrdersLink = entityModel.getLink("user-orders");
        assertTrue(userOrdersLink.isPresent());
        assertTrue(userOrdersLink.get().getHref().endsWith("/api/orders"));

        Optional<Link> userOrdersAdminLink = entityModel.getLink("user-orders-admin");
        assertTrue(userOrdersAdminLink.isPresent());
        assertTrue(userOrdersAdminLink.get().getHref().endsWith("/api/orders/admin/1"));

        Optional<Link> statusLink = entityModel.getLink("status");
        assertTrue(statusLink.isPresent());
        assertTrue(statusLink.get().getHref().endsWith("/api/orders/1/status"));

        Optional<Link> userCancelLink = entityModel.getLink("user-cancel");
        assertTrue(userCancelLink.isPresent());
        assertTrue(selfLink.get().getHref().endsWith("/api/orders/1"));

        Optional<Link> adminCancelLink = entityModel.getLink("admin-cancel");
        assertTrue(adminCancelLink.isPresent());
        assertTrue(adminCancelLink.get().getHref().endsWith("/api/orders/admin/1"));

        Optional<Link> eventLink = entityModel.getLink("event");
        assertTrue(eventLink.isPresent());
        assertTrue(eventLink.get().getHref().endsWith("/api/events/1"));
    }
}
