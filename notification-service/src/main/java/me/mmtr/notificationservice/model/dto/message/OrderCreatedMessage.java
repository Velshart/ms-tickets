package me.mmtr.notificationservice.model.dto.message;

import java.io.Serializable;

public record OrderCreatedMessage(
        Long orderId,
        Long userId,
        String eventName,
        long ticketQuantity
) implements Serializable {}
