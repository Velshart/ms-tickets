package me.mmtr.ticketservice.model.exception;

public class TicketsSoldOutException extends TicketException {
    public TicketsSoldOutException(Long eventId) {
        super("Tickets sold out for event " + eventId);
    }
}
