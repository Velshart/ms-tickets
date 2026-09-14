package me.mmtr.ticketservice.model.dto.order;

import me.mmtr.ticketservice.model.Order;
import me.mmtr.ticketservice.model.OrderStatus;
import me.mmtr.ticketservice.model.dto.ticket.TicketResponse;

import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        Long eventId,
        OrderStatus orderStatus,
        List<TicketResponse> tickets
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getEvent().getId(),
                order.getOrderStatus(),
                order.getTickets().stream().map(TicketResponse::from).toList()
        );
    }
}
