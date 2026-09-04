package me.mmtr.notificationservice.model.dto.message;

import java.io.Serializable;
import java.util.List;

public record EventStatusChangedMessage(
        String name,
        List<Long> ordersOwnersIds
) implements Serializable {}
