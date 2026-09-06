package me.mmtr.ticketservice.model.dto.ticket;

import me.mmtr.ticketservice.model.Order;

import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        Long eventId,
        List<TicketResponse> tickets
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getEvent().getId(),
                order.getTickets().stream().map(TicketResponse::from).toList()
        );
    }
}
