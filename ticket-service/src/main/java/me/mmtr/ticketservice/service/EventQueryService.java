package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.exception.EventNotFoundException;
import me.mmtr.ticketservice.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EventQueryService {
    private final EventRepository eventRepository;

    public EventQueryService(
            EventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    public EventResponse getEvent(Long id) {
        Event event = getEventEntity(id);

        return EventResponse.from(event);
    }

    public Page<EventResponse> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(EventResponse::from);
    }

//    @Transactional(noRollbackFor = TicketsSoldOutException.class)
//    public long reserveTickets(Long eventId, long quantity) {
//        Event event = getEventEntity(eventId);
//
//        long reservedTickets = event.reserveTickets(quantity);
//        eventRepository.save(event);
//
//        return reservedTickets;
//    }
//
//    @Transactional
//    public void returnTickets(Long eventId, long quantity) {
//        Event event = getEventEntity(eventId);
//        event.returnTickets(quantity);
//
//        eventRepository.save(event);
//    }

    public Event getEventEntity(Long id) {
        return getEventEntityById(id);
    }

    private Event getEventEntityById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}

