package me.mmtr.ticketservice.service;

import jakarta.transaction.Transactional;
import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.Order;
import me.mmtr.ticketservice.model.Ticket;
import me.mmtr.ticketservice.model.dto.messaging.OrderCreatedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderDeletedEvent;
import me.mmtr.ticketservice.model.dto.messaging.OrderSubmittedEvent;
import me.mmtr.ticketservice.model.dto.order.OrderResponse;
import me.mmtr.ticketservice.model.dto.order.OrderStatusResponse;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.model.exception.OrderNotFoundException;
import me.mmtr.ticketservice.model.exception.TicketSalesClosedException;
import me.mmtr.ticketservice.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final EventQueryService eventQueryService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;

    public OrderService(
            EventQueryService eventQueryService,
            OrderRepository orderRepository,
            ApplicationEventPublisher publisher
    ) {
        this.eventQueryService = eventQueryService;
        this.orderRepository = orderRepository;
        this.publisher = publisher;
    }

    @Transactional
    public OrderResponse submitOrder(
            BuyTicketsRequest request,
            Long userId
    ) {
        Event event = eventQueryService.getEventEntity(request.eventId());
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new TicketSalesClosedException(request.eventId());
        }

        Order order = new Order(userId, event);
        Order savedOrder = orderRepository.save(order);

        publisher.publishEvent(new OrderSubmittedEvent(
                savedOrder.getId(),
                userId,
                request.eventId(),
                request.ticketQuantity()
        ));
        return OrderResponse.from(savedOrder);
    }

    @Transactional
    public OrderResponse createOrder(
            BuyTicketsRequest buyTicketRequest,
            Long userId
    ) {
        Long eventId = buyTicketRequest.eventId();
        long ticketsAmount = buyTicketRequest.ticketQuantity();

        Event event = eventQueryService.getEventEntity(eventId);

        if (event.getStatus() == EventStatus.ACTIVE) {
            long reservedTicketsQuantity = eventQueryService
                    .reserveTickets(eventId, ticketsAmount);

            Order order;
            Optional<Order> orderOptional =
                    orderRepository.findByUserIdAndEventId(userId, eventId);

            order = orderOptional.orElseGet(() -> new Order(
                    userId,
                    event
            ));

            for (int i = 0; i < reservedTicketsQuantity; i++) {
                Ticket ticket = new Ticket(order);
                order.addTicket(ticket);
            }

            Order savedOrder = orderRepository.save(order);
            publisher.publishEvent(
                    new OrderCreatedEvent(
                            savedOrder.getId(),
                            userId,
                            order.getEvent().getName(),
                            reservedTicketsQuantity
                    )
            );
            return OrderResponse.from(savedOrder);
        } else {
            throw new TicketSalesClosedException(eventId);
        }
    }

    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = getOrderByIdAndUserId(orderId, userId);
        return OrderResponse.from(order);
    }

    public List<OrderResponse> getAllOrdersByEventId(Long eventId) {
        return orderRepository.findAllByEventId(eventId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    public Page<OrderResponse> getAllOrdersByUserId(
            Long userId,
            Pageable pageable
    ) {
        return orderRepository.findAllByUserId(userId, pageable)
                .map(OrderResponse::from);
    }

    @Transactional
    public void deleteOrderAsUser(Long orderId, Long userId) {
        Order order = getOrderByIdAndUserId(orderId, userId);
        deleteOrder(order);
        publisher.publishEvent(new OrderDeletedEvent(
                orderId,
                userId,
                order.getEvent().getName(),
                order.getTickets().size()
        ));
    }

    @Transactional
    public void deleteOrderAsAdmin(Long orderId) {
        Order order = getOrderById(orderId);
        deleteOrder(order);
        publisher.publishEvent(new OrderDeletedEvent(
                orderId,
                order.getUserId(),
                order.getEvent().getName(),
                order.getTickets().size()
        ));
    }

    public void deleteAllOrdersForEvent(Long eventId) {
        orderRepository.deleteAllByEventId(eventId);
    }

    private Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Order getOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void deleteOrder(Order order) {
        if (!order.getTickets().isEmpty()) {
            eventQueryService.returnTickets(
                    order.getEvent().getId(),
                    order.getTickets().size()
            );
        }

        orderRepository.delete(order);
    }

    public OrderStatusResponse getOrderStatus(Long orderId, Long userId) {
        Order order = getOrderByIdAndUserId(orderId, userId);
        return OrderStatusResponse.from(order);
    }
}