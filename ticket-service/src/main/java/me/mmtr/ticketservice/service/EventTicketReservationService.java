package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.exception.TicketsSoldOutException;
import me.mmtr.ticketservice.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventTicketReservationService {
    private final EventRepository eventRepository;
    private final EventQueryService eventQueryService;

    public EventTicketReservationService(
            EventRepository eventRepository,
            EventQueryService eventQueryService
    ) {
        this.eventRepository = eventRepository;
        this.eventQueryService = eventQueryService;
    }

    @Transactional(noRollbackFor = TicketsSoldOutException.class)
    public long reserveTickets(Long eventId, long quantity) {
        Event event = eventQueryService.getEventEntity(eventId);

        long reservedTickets = event.reserveTickets(quantity);
        eventRepository.save(event);

        return reservedTickets;
    }

    @Transactional
    public void returnTickets(Long eventId, long quantity) {
        Event event = eventQueryService.getEventEntity(eventId);
        event.returnTickets(quantity);

        eventRepository.save(event);
    }
}
