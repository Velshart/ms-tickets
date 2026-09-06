package me.mmtr.ticketservice.model.exception;

public abstract class TicketException extends RuntimeException {
    protected TicketException(String message) {
        super(message);
    }
}
