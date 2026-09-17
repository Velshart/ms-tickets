package me.mmtr.ticketservice.service;

import me.mmtr.ticketservice.model.Order;
import me.mmtr.ticketservice.model.OrderStatus;
import me.mmtr.ticketservice.model.Ticket;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderRequestMessage;
import me.mmtr.ticketservice.model.exception.OrderNotFoundException;
import me.mmtr.ticketservice.model.exception.TicketsSoldOutException;
import me.mmtr.ticketservice.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProcessingService {
    private final EventTicketReservationService eventTicketReservationService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;

    public OrderProcessingService(
            EventTicketReservationService eventTicketReservationService,
            OrderRepository orderRepository,
            ApplicationEventPublisher publisher
    ) {
        this.eventTicketReservationService = eventTicketReservationService;
        this.orderRepository = orderRepository;
        this.publisher = publisher;
    }

    @Transactional
    public void processOrder(OrderRequestMessage message) {
        Order order = orderRepository.findById(message.orderId())
                .orElseThrow(() -> new OrderNotFoundException(message.orderId()));

        if (message.containsNullValues()) {
            order.setOrderStatus(OrderStatus.PROCESSING_ERROR);
            orderRepository.save(order);
            return;
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            return;
        }

        try {
            long reservedTicketsQuantity = eventTicketReservationService
                    .reserveTickets(message.eventId(), message.ticketQuantity());

            for (int i = 0; i < reservedTicketsQuantity; i++) {
                Ticket ticket = new Ticket(order);
                order.addTicket(ticket);
            }
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            publisher.publishEvent(
                    new OrderCreatedEvent(
                            order.getId(),
                            message.userId(),
                            order.getEvent().getName(),
                            reservedTicketsQuantity
                    )
            );
        } catch (TicketsSoldOutException e) {
            order.setOrderStatus(OrderStatus.TICKETS_SOLD_OUT);
            orderRepository.save(order);
        } catch (ConcurrencyFailureException e) {
            order.setOrderStatus(OrderStatus.PROCESSING_ERROR);
            orderRepository.save(order);
        }
    }
}