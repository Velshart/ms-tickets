package me.mmtr.ticketservice.model.dto.event;

import jakarta.validation.constraints.NotNull;
import me.mmtr.ticketservice.model.EventStatus;

public record ChangeEventStatusRequest(
        @NotNull(message = "Status cannot be null")
        EventStatus status
) {}
