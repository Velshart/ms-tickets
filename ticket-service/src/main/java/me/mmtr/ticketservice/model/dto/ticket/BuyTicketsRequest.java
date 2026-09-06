package me.mmtr.ticketservice.model.dto.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BuyTicketsRequest(
        @NotNull(message = "Event ID cannot be null.")
        @Positive(message = "Event ID must be positive.")
        Long eventId,
        @NotNull(message = "Number of tickets cannot be null.")
        @Positive(message = "Number of tickets must be positive.")
        long tickets
) {}
