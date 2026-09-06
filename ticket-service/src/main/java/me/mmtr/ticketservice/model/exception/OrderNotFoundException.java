package me.mmtr.ticketservice.model.exception;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(Long id) {
        super("Order with id " + id + " not found");
    }
}
