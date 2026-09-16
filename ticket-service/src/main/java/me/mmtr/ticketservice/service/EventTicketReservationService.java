package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.exception.TicketsSoldOutException;
import me.mmtr.ticketservice.repository.EventRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional(
            noRollbackFor = TicketsSoldOutException.class,
            propagation = Propagation.REQUIRES_NEW
    )
    @Retryable(
            retryFor = {
                    ObjectOptimisticLockingFailureException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000L, random = true)
    )
    public long reserveTickets(Long eventId, long quantity) {
        Event event = eventQueryService.getEventEntity(eventId);

        long reservedTickets = event.reserveTickets(quantity);
        eventRepository.save(event);

        return reservedTickets;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {
                    ObjectOptimisticLockingFailureException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000L, random = true)
    )
    public void returnTickets(Long eventId, long quantity) {
        Event event = eventQueryService.getEventEntity(eventId);
        event.returnTickets(quantity);

        eventRepository.save(event);
    }
}
