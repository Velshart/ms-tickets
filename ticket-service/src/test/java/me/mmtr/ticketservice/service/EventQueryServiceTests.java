package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.exception.EventNotFoundException;
import me.mmtr.ticketservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventQueryServiceTests {

    private static final Long EVENT_ID = 1L;
    private static final String EVENT_NAME = "Test Festival";
    private static final Long INITIAL_TICKETS = 100L;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventQueryService eventQueryService;

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(EVENT_ID);
        event.setName(EVENT_NAME);
        event.setAvailableTickets(INITIAL_TICKETS);
    }

    @Nested
    class GetEventTests {

        @Test
        void shouldReturnEventResponseWhenEventExists() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            EventResponse response = eventQueryService.getEvent(EVENT_ID);

            assertNotNull(response);
            assertEquals(EVENT_ID, response.id());
            assertEquals(EVENT_NAME, response.name());
        }

        @Test
        void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () ->
                    eventQueryService.getEvent(EVENT_ID)
            );
        }

        @Test
        void shouldReturnPagedEvents() {
            Event event2 = new Event("Second Event",
                    BigDecimal.valueOf(10),
                    OffsetDateTime.now().plusDays(1),
                    OffsetDateTime.now().plusDays(2),
                    500L);
            event2.setName("Second Event");

            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> page = new PageImpl<>(List.of(event, event2), pageable, 2);

            when(eventRepository.findAll(pageable)).thenReturn(page);

            Page<EventResponse> result = eventQueryService.getAllEvents(pageable);

            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getContent().size());
            verify(eventRepository).findAll(pageable);
        }

        @Test
        void shouldReturnEventEntityWhenEventExists() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Event entity = eventQueryService.getEventEntity(EVENT_ID);

            assertNotNull(entity);
            assertEquals(EVENT_ID, entity.getId());
        }
    }

    @Nested
    class TicketReservationTests {
        private static final long QUANTITY_TO_RESERVE = 5L;
        private static final long QUANTITY_TO_RETURN = 3L;

        @Test
        void shouldReserveTicketsAndSaveEvent() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(event)).thenReturn(event);

            long reserved = eventQueryService.reserveTickets(EVENT_ID, QUANTITY_TO_RESERVE);

            assertEquals(QUANTITY_TO_RESERVE, reserved);
            assertEquals(INITIAL_TICKETS - QUANTITY_TO_RESERVE, event.getAvailableTickets());
            verify(eventRepository).save(event);
        }

        @Test
        void shouldReturnTicketsAndSaveEvent() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(event)).thenReturn(event);

            eventQueryService.returnTickets(EVENT_ID, QUANTITY_TO_RETURN);

            assertEquals(INITIAL_TICKETS + QUANTITY_TO_RETURN, event.getAvailableTickets());
            verify(eventRepository).save(event);
        }
    }
}