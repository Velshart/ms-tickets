package me.mmtr.ticketservice.model.dto.event;

import jakarta.validation.constraints.*;
import me.mmtr.ticketservice.model.dto.event.validator.ValidEventDates;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@ValidEventDates
public record CreateEventRequest(
        @NotBlank(message = "Name cannot be blank.")
        @Size(max = 100, message = "Name cannot exceed 100 characters.")
        String name,

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
        BigDecimal price,

        @NotNull(message = "Start date cannot be null")
        @Future(message = "Start date must be in the future")
        OffsetDateTime startDate,

        @NotNull(message = "End date cannot be null")
        @Future(message = "End date must be in the future")
        OffsetDateTime endDate,

        @NotNull(message = "Number of available tickets cannot be null")
        @PositiveOrZero(message = "Number of available tickets cannot be negative")
        Long availableTickets
) {
}
