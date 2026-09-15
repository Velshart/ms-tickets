package me.mmtr.ticketservice.model.dto.event;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.EventStatus;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Relation(collectionRelation = "events", itemRelation = "event")
public record EventResponse(
        Long id,
        String name,
        BigDecimal price,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        EventStatus status,
        long availableTickets
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getPrice(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStatus(),
                event.getAvailableTickets()
        );
    }
}
