package me.mmtr.ticketservice.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "order_seq",
            sequenceName = "order_id_seq",
            allocationSize = 100
    )
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Event event;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private final List<Ticket> tickets = new ArrayList<>();

    public Order() {
    }

    public Order(Long userId, Event event) {
        this.userId = userId;
        this.event = event;
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }
}
