package me.mmtr.ticketservice.model.exception;

public class EventNotFoundException extends NotFoundException {
    public EventNotFoundException(Long id) {
        super("Event with id " + id + " not found");
    }
}
