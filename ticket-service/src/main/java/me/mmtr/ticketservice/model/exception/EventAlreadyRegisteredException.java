package me.mmtr.ticketservice.model.exception;

import me.mmtr.common.web.ConflictException;

public class EventAlreadyRegisteredException extends ConflictException {
    public EventAlreadyRegisteredException(String name) {
        super("Event with name " + name + " is already registered");
    }
}
