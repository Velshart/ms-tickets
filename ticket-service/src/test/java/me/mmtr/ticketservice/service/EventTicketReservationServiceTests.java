package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventTicketReservationServiceTests {
    private static final long QUANTITY_TO_RESERVE = 5L;
    private static final long QUANTITY_TO_RETURN = 3L;
    private static final Long EVENT_ID = 1L;
    private static final String EVENT_NAME = "Test Festival";
    private static final Long INITIAL_TICKETS = 100L;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventQueryService eventQueryService;
    @InjectMocks
    private EventTicketReservationService eventTicketReservationService;
    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(EVENT_ID);
        event.setName(EVENT_NAME);
        event.setAvailableTickets(INITIAL_TICKETS);
    }


    @Nested
    class TicketReservationTests {
        @Test
        void shouldReserveTicketsAndSaveEvent() {
            when(eventQueryService.getEventEntity(EVENT_ID)).thenReturn(event);
            when(eventRepository.save(event)).thenReturn(event);

            long reserved = eventTicketReservationService.reserveTickets(
                    EVENT_ID,
                    QUANTITY_TO_RESERVE
            );

            assertEquals(QUANTITY_TO_RESERVE, reserved);
            assertEquals(
                    INITIAL_TICKETS - QUANTITY_TO_RESERVE,
                    event.getAvailableTickets()
            );
            verify(eventRepository).save(event);
        }

        @Test
        void shouldReturnTicketsAndSaveEvent() {
            when(eventQueryService.getEventEntity(EVENT_ID)).thenReturn(event);
            when(eventRepository.save(event)).thenReturn(event);

            eventTicketReservationService.returnTickets(
                    EVENT_ID,
                    QUANTITY_TO_RETURN
            );

            assertEquals(
                    INITIAL_TICKETS + QUANTITY_TO_RETURN,
                    event.getAvailableTickets()
            );
            verify(eventRepository).save(event);
        }
    }
}
