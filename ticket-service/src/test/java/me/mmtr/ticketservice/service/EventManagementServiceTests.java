package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateEventRequest;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.dto.event.UpdateEventRequest;
import me.mmtr.ticketservice.model.dto.messaging.EventDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventStatusChangedEvent;
import me.mmtr.ticketservice.model.dto.messaging.EventUpdatedEvent;
import me.mmtr.ticketservice.model.dto.ticket.OrderResponse;
import me.mmtr.ticketservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventManagementServiceTests {

    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID_1 = 100L;
    private static final Long USER_ID_2 = 101L;

    private static final String OLD_NAME = "Old Name";
    private static final BigDecimal OLD_PRICE = BigDecimal.valueOf(50);
    private static final long OLD_TICKETS = 100L;

    private static final String NEW_NAME = "New Name";
    private static final BigDecimal NEW_PRICE = BigDecimal.valueOf(75);
    private static final long NEW_TICKETS = 150L;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private OrderService orderService;
    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private EventManagementService eventManagementService;

    private Event existingEvent;

    @BeforeEach
    void setUp() {
        existingEvent = new Event();
        existingEvent.setId(EVENT_ID);
        existingEvent.setName(OLD_NAME);
        existingEvent.setPrice(OLD_PRICE);
        existingEvent.setAvailableTickets(OLD_TICKETS);
        existingEvent.setStatus(EventStatus.ACTIVE);
    }

    @Nested
    class CreateAndUpdateTests {

        @Test
        void shouldCreateEvent() {
            CreateEventRequest request = new CreateEventRequest(
                    NEW_NAME,
                    NEW_PRICE,
                    OffsetDateTime.now(),
                    OffsetDateTime.now().plusDays(1),
                    NEW_TICKETS
            );

            when(eventRepository.save(any(Event.class)))
                    .thenAnswer(inv -> {
                        Event e = inv.getArgument(0);
                        e.setId(EVENT_ID);
                        return e;
                    });

            EventResponse response = eventManagementService.createEvent(request);

            assertNotNull(response);
            assertEquals(EVENT_ID, response.id());
            assertEquals(NEW_NAME, response.name());
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        void shouldUpdateEventAndPublishEventUpdatedEvent() {
            UpdateEventRequest request = new UpdateEventRequest(
                    NEW_NAME,
                    NEW_PRICE,
                    OffsetDateTime.now(),
                    OffsetDateTime.now().plusDays(1),
                    null
            );

            OrderResponse mockOrder1 = mock(OrderResponse.class);
            OrderResponse mockOrder2 = mock(OrderResponse.class);
            when(mockOrder1.userId()).thenReturn(USER_ID_1);
            when(mockOrder2.userId()).thenReturn(USER_ID_2);

            when(eventQueryService.getEventEntity(EVENT_ID))
                    .thenReturn(existingEvent);
            when(orderService.getAllOrdersByEventId(EVENT_ID))
                    .thenReturn(List.of(mockOrder1, mockOrder2));
            when(eventRepository.save(any(Event.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EventResponse response =
                    eventManagementService.updateEvent(request, EVENT_ID);

            assertNotNull(response);
            assertEquals(NEW_NAME, response.name());

            ArgumentCaptor<EventUpdatedEvent> captor = ArgumentCaptor.captor();
            verify(publisher).publishEvent(captor.capture());

            EventUpdatedEvent publishedEvent = captor.getValue();
            assertEquals(OLD_NAME, publishedEvent.oldName());
            assertEquals(NEW_NAME, publishedEvent.newName());
            assertEquals(
                    List.of(USER_ID_1, USER_ID_2),
                    publishedEvent.ordersOwnersIds()
            );
        }
    }

    @Nested
    class StatusAndDeleteTests {

        @Test
        void shouldChangeEventStatusAndPublishEvent() {
            ChangeEventStatusRequest request = new ChangeEventStatusRequest(EventStatus.SALES_CLOSED);

            when(eventQueryService.getEventEntity(EVENT_ID)).thenReturn(existingEvent);
            when(orderService.getAllOrdersByEventId(EVENT_ID)).thenReturn(Collections.emptyList());
            when(eventRepository.save(existingEvent)).thenReturn(existingEvent);

            EventResponse response = eventManagementService.changeEventStatus(request, EVENT_ID);

            assertNotNull(response);
            assertEquals(EventStatus.SALES_CLOSED, existingEvent.getStatus());

            ArgumentCaptor<EventStatusChangedEvent> captor = ArgumentCaptor.captor();
            verify(publisher).publishEvent(captor.capture());
            assertEquals(OLD_NAME, captor.getValue().name());
        }

        @Test
        void shouldDeleteEventAndOrdersAndPublishEvent() {
            OrderResponse mockOrder = mock(OrderResponse.class);
            when(mockOrder.userId()).thenReturn(USER_ID_1);

            when(eventQueryService.getEventEntity(EVENT_ID)).thenReturn(existingEvent);
            when(orderService.getAllOrdersByEventId(EVENT_ID)).thenReturn(List.of(mockOrder));

            eventManagementService.deleteEventAndOrders(EVENT_ID);

            verify(orderService).deleteAllOrdersForEvent(EVENT_ID);
            verify(eventRepository).delete(existingEvent);

            ArgumentCaptor<EventDeletedEvent> captor = ArgumentCaptor.captor();
            verify(publisher).publishEvent(captor.capture());

            EventDeletedEvent publishedEvent = captor.getValue();
            assertEquals(OLD_NAME, publishedEvent.name());
            assertEquals(List.of(USER_ID_1), publishedEvent.ordersOwnersIds());
        }
    }
}
