package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;

public record OrderSubmittedEvent(
        Long orderId,
        Long userId,
        Long eventId,
        Long ticketQuantity
) implements Serializable {}
