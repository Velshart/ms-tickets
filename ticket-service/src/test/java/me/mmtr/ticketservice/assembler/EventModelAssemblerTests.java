package me.mmtr.ticketservice.assembler;

import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EventModelAssemblerTests {
    private final EventModelAssembler eventModelAssembler = new EventModelAssembler();

    @Test
    void shouldCreateEntityModelWithCorrectLinks() {
        EventResponse eventResponse = new EventResponse(
                1L,
                "Event",
                BigDecimal.TEN,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                EventStatus.ACTIVE,
                100L
        );

        EntityModel<EventResponse> entityModel =
                eventModelAssembler.toModel(eventResponse);

        assertNotNull(entityModel);
        assertEquals(entityModel.getContent(), eventResponse);

        Optional<Link> selfLink = entityModel.getLink("self");
        assertTrue(selfLink.isPresent());
        assertTrue(selfLink.get().getHref().endsWith("/api/events/1"));

        Optional<Link> eventsLink = entityModel.getLink("events");
        assertTrue(eventsLink.isPresent());
        assertTrue(eventsLink.get().getHref().endsWith("/api/events"));

        Optional<Link> updateLink = entityModel.getLink("update");
        assertTrue(updateLink.isPresent());
        assertTrue(updateLink.get().getHref().endsWith("/api/events/1"));

        Optional<Link> updateStatusLink = entityModel.getLink("update-status");
        assertTrue(updateStatusLink.isPresent());
        assertTrue(updateStatusLink.get().getHref().endsWith("/api/events/1/status"));

        Optional<Link> deleteLink = entityModel.getLink("delete");
        assertTrue(deleteLink.isPresent());
        assertTrue(deleteLink.get().getHref().endsWith("/api/events/1"));
    }
}
