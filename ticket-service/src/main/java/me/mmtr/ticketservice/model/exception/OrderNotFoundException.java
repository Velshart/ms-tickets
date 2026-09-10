package me.mmtr.ticketservice.model.exception;

import me.mmtr.common.web.NotFoundException;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(Long id) {
        super("Order with id " + id + " not found");
    }
}
