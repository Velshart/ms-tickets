package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;
import java.util.List;

public record EventStatusChangedEvent(
        String name,
        List<Long> ordersOwnersIds
) implements Serializable {}
