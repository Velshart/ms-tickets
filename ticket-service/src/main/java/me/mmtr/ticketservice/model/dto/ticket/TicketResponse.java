package me.mmtr.ticketservice.model.dto.ticket;

import me.mmtr.ticketservice.model.Ticket;

import java.util.UUID;

public record TicketResponse(UUID code) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getCode());
    }
}
