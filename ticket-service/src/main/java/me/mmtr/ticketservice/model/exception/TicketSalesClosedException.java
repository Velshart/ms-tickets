package me.mmtr.ticketservice.model.exception;

import me.mmtr.common.web.ConflictException;

public class TicketSalesClosedException extends ConflictException {
    public TicketSalesClosedException(Long id) {
        super(
                "Ticket sales closed for event with id " + id
        );
    }
}
