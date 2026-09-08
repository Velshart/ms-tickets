package me.mmtr.ticketservice.model.dto.messaging;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record EventUpdatedEvent(
        String oldName,
        BigDecimal oldPrice,
        OffsetDateTime oldStartDate,
        OffsetDateTime oldEndDate,
        long oldAvailableTickets,
        String newName,
        BigDecimal newPrice,
        OffsetDateTime newStartDate,
        OffsetDateTime newEndDate,
        long newAvailableTickets,
        List<Long> ordersOwnersIds
) implements Serializable {}
