package me.mmtr.ticketservice;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.Order;
import me.mmtr.ticketservice.model.Ticket;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.model.dto.ticket.OrderResponse;
import me.mmtr.ticketservice.model.exception.OrderNotFoundException;
import me.mmtr.ticketservice.model.exception.TicketSalesClosedException;
import me.mmtr.ticketservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTests {

    private static final Long USER_ID = 100L;
    private static final Long ACTIVE_EVENT_ID = 1L;
    private static final Long INACTIVE_EVENT_ID = 2L;
    private static final Long ORDER_ID = 10L;
    private static final Long SAVED_ORDER_ID = 500L;

    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private OrderService orderService;

    private Event activeEvent;
    private Event inactiveEvent;

    @BeforeEach
    void setUp() {
        activeEvent = new Event();
        activeEvent.setId(ACTIVE_EVENT_ID);
        activeEvent.setName("Active Event");
        activeEvent.setStatus(EventStatus.ACTIVE);

        inactiveEvent = new Event();
        inactiveEvent.setId(INACTIVE_EVENT_ID);
        inactiveEvent.setName("Closed Event");
        inactiveEvent.setStatus(EventStatus.SALES_CLOSED);
    }

    @Nested
    class CreateOrderTests {

        @Test
        void shouldCreateNewOrderAndPublishEventWhenOrderDoesNotExist() {
            BuyTicketsRequest request = new BuyTicketsRequest(ACTIVE_EVENT_ID, 2L);

            when(eventQueryService.getEventEntity(ACTIVE_EVENT_ID)).thenReturn(activeEvent);
            when(eventQueryService.reserveTickets(ACTIVE_EVENT_ID, 2L)).thenReturn(2L);
            when(orderRepository.findByUserIdAndEventId(USER_ID, ACTIVE_EVENT_ID)).thenReturn(Optional.empty());

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(SAVED_ORDER_ID);
                return orderToSave;
            });

            OrderResponse response = orderService.createOrder(request, USER_ID);

            assertNotNull(response);
            assertEquals(SAVED_ORDER_ID, response.id());
            assertEquals(2, response.tickets().size());

            verify(orderRepository).save(any(Order.class));

            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.captor();
            verify(publisher).publishEvent(eventCaptor.capture());

            OrderCreatedEvent publishedEvent = eventCaptor.getValue();
            assertEquals(SAVED_ORDER_ID, publishedEvent.id());
            assertEquals(USER_ID, publishedEvent.userId());
            assertEquals("Active Event", publishedEvent.eventName());
            assertEquals(2L, publishedEvent.ticketQuantity());
        }

        @Test
        void shouldAddTicketsToExistingOrderWhenOrderAlreadyExists() {
            BuyTicketsRequest request = new BuyTicketsRequest(ACTIVE_EVENT_ID, 2L);

            Order existingOrder = new Order(USER_ID, activeEvent);
            existingOrder.setId(SAVED_ORDER_ID);
            existingOrder.addTicket(new Ticket(existingOrder));

            when(eventQueryService.getEventEntity(ACTIVE_EVENT_ID)).thenReturn(activeEvent);
            when(eventQueryService.reserveTickets(ACTIVE_EVENT_ID, 2L)).thenReturn(2L);
            when(orderRepository.findByUserIdAndEventId(USER_ID, ACTIVE_EVENT_ID)).thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(existingOrder)).thenReturn(existingOrder);

            OrderResponse response = orderService.createOrder(request, USER_ID);

            assertNotNull(response);
            assertEquals(3, response.tickets().size());
            verify(orderRepository).save(existingOrder);
            verify(publisher).publishEvent(any(OrderCreatedEvent.class));
        }

        @Test
        void shouldThrowTicketSalesClosedExceptionWhenEventIsNotActive() {
            BuyTicketsRequest request = new BuyTicketsRequest(INACTIVE_EVENT_ID, 2L);

            when(eventQueryService.getEventEntity(INACTIVE_EVENT_ID)).thenReturn(inactiveEvent);

            assertThrows(TicketSalesClosedException.class, () ->
                    orderService.createOrder(request, USER_ID)
            );

            verify(eventQueryService, never()).reserveTickets(anyLong(), anyLong());
            verify(orderRepository, never()).save(any(Order.class));
            verify(publisher, never()).publishEvent(any());
        }
    }

    @Nested
    class GetOrderTests {

        @Test
        void shouldReturnOrderResponseWhenOrderExists() {
            Order order = new Order(USER_ID, activeEvent);
            order.setId(ORDER_ID);

            when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

            OrderResponse response = orderService.getOrder(ORDER_ID, USER_ID);

            assertNotNull(response);
            assertEquals(ORDER_ID, response.id());
        }

        @Test
        void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
            when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () ->
                    orderService.getOrder(ORDER_ID, USER_ID)
            );
        }

        @Test
        void shouldReturnAllOrdersByUserId() {
            Order order1 = new Order(USER_ID, activeEvent);
            order1.setId(1L);
            Order order2 = new Order(USER_ID, activeEvent);
            order2.setId(2L);

            when(orderRepository.findAllByUserId(USER_ID)).thenReturn(List.of(order1, order2));

            List<OrderResponse> result = orderService.getAllOrdersByUserId(USER_ID);

            assertEquals(2, result.size());
            verify(orderRepository).findAllByUserId(USER_ID);
        }
    }

    @Nested
    class DeleteOrderTests {

        @Test
        void shouldDeleteOrderAsUserAndPublishEvent() {
            Order order = new Order(USER_ID, activeEvent);
            order.setId(ORDER_ID);
            order.addTicket(new Ticket(order));
            order.addTicket(new Ticket(order));

            when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

            orderService.deleteOrderAsUser(ORDER_ID, USER_ID);

            verify(eventQueryService).returnTickets(activeEvent.getId(), 2);
            verify(orderRepository).delete(order);

            ArgumentCaptor<OrderDeletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderDeletedEvent.class);
            verify(publisher).publishEvent(eventCaptor.capture());

            OrderDeletedEvent publishedEvent = eventCaptor.getValue();
            assertEquals(ORDER_ID, publishedEvent.id());
            assertEquals(USER_ID, publishedEvent.userId());
            assertEquals(2, publishedEvent.ticketQuantity());
        }

        @Test
        void shouldDeleteOrderAsAdminAndPublishEvent() {
            Order order = new Order(USER_ID, activeEvent);
            order.setId(ORDER_ID);
            order.addTicket(new Ticket(order));

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            orderService.deleteOrderAsAdmin(ORDER_ID);

            verify(eventQueryService).returnTickets(activeEvent.getId(), 1);
            verify(orderRepository).delete(order);

            ArgumentCaptor<OrderDeletedEvent> eventCaptor = ArgumentCaptor.captor();
            verify(publisher).publishEvent(eventCaptor.capture());

            OrderDeletedEvent publishedEvent = eventCaptor.getValue();
            assertEquals(ORDER_ID, publishedEvent.id());
            assertEquals(USER_ID, publishedEvent.userId());
        }

        @Test
        void shouldDeleteAllOrdersForEvent() {
            orderService.deleteAllOrdersForEvent(ACTIVE_EVENT_ID);

            verify(orderRepository).deleteAllByEventId(ACTIVE_EVENT_ID);
        }
    }
}