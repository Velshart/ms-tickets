package me.mmtr.ticketservice.model.dto.event;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateEventRequest(
        @Size(max = 100, message = "Name cannot exceed 100 characters.")
        String name,

        @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
        BigDecimal price,

        @Future(message = "Start date must be in the future")
        OffsetDateTime startDate,

        @Future(message = "End date must be in the future")
        OffsetDateTime endDate,

        @PositiveOrZero(message = "Number of available tickets cannot be negative")
        Long availableTickets
) {}
