package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;

public record OrderDeletedEvent(
        Long id,
        Long userId,
        String eventName,
        long ticketQuantity
) implements Serializable {}