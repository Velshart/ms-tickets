package me.mmtr.ticketservice.model.exception;

import me.mmtr.common.web.ConflictException;

public abstract class TicketException extends RuntimeException {
    protected TicketException(String message) {
        super(message);
    }
}
