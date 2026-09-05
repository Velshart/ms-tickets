package me.mmtr.ticketservice;

import jakarta.transaction.Transactional;
import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.exception.EventNotFoundException;
import me.mmtr.ticketservice.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventQueryService {
    private final EventRepository eventRepository;

    public EventQueryService(
            EventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    public EventResponse getEvent(Long id) {
        Event event = getEventById(id);

        return EventResponse.from(event);
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional
    public long reserveTickets(Long eventId, long quantity) {
        Event event = getEventById(eventId);

        long reservedTickets = event.reserveTickets(quantity);
        eventRepository.save(event);

        return reservedTickets;
    }

    @Transactional
    public void returnTickets(Long eventId, long quantity) {
        Event event = getEventEntity(eventId);
        event.returnTickets(quantity);

        eventRepository.save(event);
    }

    public Event getEventEntity(Long id) {
        return getEventById(id);
    }

    private Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}

