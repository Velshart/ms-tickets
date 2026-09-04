package me.mmtr.notificationservice.model.dto.message;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record EventUpdatedMessage(
        String oldName,
        BigDecimal oldPrice,
        long oldAvailableTickets,
        String newName,
        BigDecimal newPrice,
        long newAvailableTickets,
        List<Long> ordersOwnersIds
) implements Serializable {}
