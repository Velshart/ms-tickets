package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;

public record OrderCreatedEvent(
        Long id,
        Long userId,
        String eventName,
        long ticketQuantity
) implements Serializable {}
