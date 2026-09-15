package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.Order;
import me.mmtr.ticketservice.model.OrderStatus;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderRequestMessage;
import me.mmtr.ticketservice.model.exception.OrderNotFoundException;
import me.mmtr.ticketservice.model.exception.TicketsSoldOutException;
import me.mmtr.ticketservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderProcessingServiceTests {
    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 2L;
    private static final String EVENT_NAME = "Event";
    private static final Long EVENT_ID = 3L;
    private static final Long TICKET_QUANTITY = 300L;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private ApplicationEventPublisher publisher;
    @InjectMocks
    private OrderProcessingService orderProcessingService;
    private Order order;
    private OrderRequestMessage orderRequestMessage;

    @BeforeEach
    void setUp() {
        Event event = new Event(
                EVENT_NAME,
                BigDecimal.TEN,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                TICKET_QUANTITY
        );
        event.setId(EVENT_ID);

        order = new Order(USER_ID, event);
        order.setId(ORDER_ID);

        orderRequestMessage = new OrderRequestMessage(
                ORDER_ID,
                USER_ID,
                EVENT_ID,
                TICKET_QUANTITY
        );
    }

    @Test
    void shouldThrowOrderNotFoundExceptionWhenOrderNotFound() {
        when(orderRepository.findById(ORDER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderProcessingService.processOrder(orderRequestMessage)
        );

        verify(orderRepository, never()).save(any(Order.class));
        verify(publisher, never()).publishEvent(any(OrderCreatedEvent.class));
        verify(eventQueryService, never()).reserveTickets(anyLong(), anyLong());
    }

    @Test
    void shouldSetProcessingErrorStatusWhenMessageContainsNullValues() {
        OrderRequestMessage invalidMessage = new OrderRequestMessage(
                ORDER_ID,
                null,
                EVENT_ID,
                TICKET_QUANTITY
        );
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        orderProcessingService.processOrder(invalidMessage);

        assertEquals(OrderStatus.PROCESSING_ERROR, order.getOrderStatus());
        verify(orderRepository).save(order);
        verify(eventQueryService, never()).reserveTickets(anyLong(), anyLong());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void shouldDoNothingWhenOrderStatusIsNotPending() {
        order.setOrderStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        orderProcessingService.processOrder(orderRequestMessage);

        verify(orderRepository, never()).save(any(Order.class));
        verify(eventQueryService, never()).reserveTickets(anyLong(), anyLong());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void shouldConfirmOrderAddTicketsAndPublishEventWhenTicketsReserved() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(eventQueryService.reserveTickets(EVENT_ID, TICKET_QUANTITY)).thenReturn(TICKET_QUANTITY);

        orderProcessingService.processOrder(orderRequestMessage);

        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        assertEquals(TICKET_QUANTITY.intValue(), order.getTickets().size());

        verify(orderRepository).save(order);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.captor();
        verify(publisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(ORDER_ID, publishedEvent.orderId());
        assertEquals(USER_ID, publishedEvent.userId());
        assertEquals(EVENT_NAME, publishedEvent.eventName());
        assertEquals(TICKET_QUANTITY, publishedEvent.ticketQuantity());
    }

    @Test
    void shouldSetTicketsSoldOutStatusWhenTicketsSoldOutExceptionThrown() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(eventQueryService.reserveTickets(EVENT_ID, TICKET_QUANTITY))
                .thenThrow(new TicketsSoldOutException(EVENT_ID));

        orderProcessingService.processOrder(orderRequestMessage);

        assertEquals(OrderStatus.TICKETS_SOLD_OUT, order.getOrderStatus());
        verify(orderRepository).save(order);
        verify(publisher, never()).publishEvent(any());
    }
}
