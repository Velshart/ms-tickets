package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;

public record OrderRequestMessage(
        Long orderId,
        Long userId,
        Long eventId,
        Long ticketQuantity
) implements Serializable {
    public boolean containsNullValues() {
        return orderId == null ||
                userId == null ||
                eventId == null ||
                ticketQuantity == null;
    }
}
