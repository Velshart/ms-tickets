package me.mmtr.ticketservice.model.exception;

public class EventAlreadyRegisteredException extends RuntimeException {
    public EventAlreadyRegisteredException(String name) {
        super("Event with name "  + name + " is already registered");
    }
}
