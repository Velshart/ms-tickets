package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String eventName,
        long ticketQuantity
) implements Serializable {}
