package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record EventUpdatedEvent(
        String oldName,
        BigDecimal oldPrice,
        long oldAvailableTickets,
        String newName,
        BigDecimal newPrice,
        long newAvailableTickets,
        List<Long> ordersOwnersIds
) implements Serializable {}
