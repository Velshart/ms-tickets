package me.mmtr.ticketservice.service;

import jakarta.transaction.Transactional;
import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateEventRequest;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.dto.event.UpdateEventRequest;
import me.mmtr.ticketservice.model.dto.messaging.EventDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventStatusChangedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventUpdatedEvent;
import me.mmtr.ticketservice.model.dto.ticket.OrderResponse;
import me.mmtr.ticketservice.model.exception.EventAlreadyRegisteredException;
import me.mmtr.ticketservice.repository.EventRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class EventManagementService {
    private final EventRepository eventRepository;
    private final EventQueryService eventQueryService;
    private final OrderService orderService;
    private final ApplicationEventPublisher publisher;

    public EventManagementService(
            EventRepository eventRepository,
            EventQueryService eventQueryService,
            OrderService orderService,
            ApplicationEventPublisher publisher
    ) {
        this.eventRepository = eventRepository;
        this.eventQueryService = eventQueryService;
        this.orderService = orderService;
        this.publisher = publisher;
    }

    public EventResponse createEvent(CreateEventRequest eventRequest) {
        if (!eventRepository.existsByName(eventRequest.name())) {
            Event event = new Event(
                    eventRequest.name(),
                    eventRequest.price(),
                    eventRequest.startDate(),
                    eventRequest.endDate(),
                    eventRequest.availableTickets()
            );
            return EventResponse.from(eventRepository.save(event));
        } else {
            throw new EventAlreadyRegisteredException(eventRequest.name());
        }
    }

    @Transactional
    public EventResponse updateEvent(
            UpdateEventRequest eventRequest,
            Long id
    ) {
        String updatedName = eventRequest.name();
        BigDecimal updatedPrice = eventRequest.price();
        OffsetDateTime updatedStartDate = eventRequest.startDate();
        OffsetDateTime updatedEndDate = eventRequest.endDate();
        Long updatedAvailableTickets = eventRequest.availableTickets();

        Event event = eventQueryService.getEventEntity(id);

        String oldName = event.getName();
        BigDecimal oldPrice = event.getPrice();
        OffsetDateTime oldStartDate = event.getStartDate();
        OffsetDateTime oldEndDate = event.getEndDate();
        long oldAvailableTickets = event.getAvailableTickets();

        if (updatedName != null) {
            event.setName(updatedName);
        }

        if (updatedPrice != null) {
            event.setPrice(updatedPrice);
        }

        if (updatedStartDate != null) {
            event.setStartDate(updatedStartDate);
        }

        if (updatedEndDate != null) {
            event.setEndDate(updatedEndDate);
        }

        if (updatedAvailableTickets != null) {
            event.setAvailableTickets(updatedAvailableTickets);
        }

        Event savedEvent = eventRepository.save(event);

        publisher.publishEvent(new EventUpdatedEvent(
                oldName,
                oldPrice,
                oldStartDate,
                oldEndDate,
                oldAvailableTickets,
                savedEvent.getName(),
                savedEvent.getPrice(),
                savedEvent.getStartDate(),
                savedEvent.getEndDate(),
                savedEvent.getAvailableTickets(),
                extractUserIdsFromOrdersByEventId(id)
        ));

        return EventResponse.from(savedEvent);
    }

    @Transactional
    public EventResponse changeEventStatus(
            ChangeEventStatusRequest changeRequest,
            Long id
    ) {
        Event event = eventQueryService.getEventEntity(id);
        event.setStatus(changeRequest.status());

        Event savedEvent = eventRepository.save(event);
        publisher.publishEvent(new EventStatusChangedEvent(
                event.getName(),
                extractUserIdsFromOrdersByEventId(id)
        ));

        return EventResponse.from(savedEvent);
    }

    @Transactional
    public void deleteEventAndOrders(Long id) {
        Event event = eventQueryService.getEventEntity(id);
        List<Long> ordersOwnersIds = extractUserIdsFromOrdersByEventId(id);

        orderService.deleteAllOrdersForEvent(id);
        deleteEvent(id);

        publisher.publishEvent(
                new EventDeletedEvent(
                        event.getName(),
                        ordersOwnersIds
                )
        );
    }

    void deleteEvent(Long id) {
        Event event = eventQueryService.getEventEntity(id);
        eventRepository.delete(event);
    }

    private List<Long> extractUserIdsFromOrdersByEventId(Long eventId) {
        return orderService.getAllOrdersByEventId(eventId)
                .stream()
                .map(OrderResponse::userId)
                .toList();
    }
}
