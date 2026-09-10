package me.mmtr.ticketservice.model.exception;

import me.mmtr.common.web.ConflictException;

public class TicketsSoldOutException extends ConflictException {
    public TicketsSoldOutException(Long eventId) {
        super("Tickets sold out for event " + eventId);
    }
}
