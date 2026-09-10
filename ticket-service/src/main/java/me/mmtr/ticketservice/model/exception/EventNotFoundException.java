package me.mmtr.ticketservice.model.exception;

import me.mmtr.common.web.NotFoundException;

public class EventNotFoundException extends NotFoundException {
    public EventNotFoundException(Long id) {
        super("Event with id " + id + " not found");
    }
}
