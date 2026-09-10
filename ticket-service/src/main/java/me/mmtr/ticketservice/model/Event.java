package me.mmtr.ticketservice.model;

import jakarta.persistence.*;
import me.mmtr.ticketservice.model.exception.TicketsSoldOutException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "event_seq",
            sequenceName = "event_id_seq",
            allocationSize = 100
    )
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private OffsetDateTime startDate;
    @Column(nullable = false)
    private OffsetDateTime endDate;

    @Column(nullable = false)
    private Long availableTickets;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Version
    private Long version;

    public Event() {}

    public Event(
            String name,
            BigDecimal price,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            long availableTickets
    ) {
        this.name = name;
        this.price = price;
        this.startDate = startDate;
        this.endDate = endDate;
        this.availableTickets = availableTickets;
        this.status = EventStatus.SALES_CLOSED;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public Long getAvailableTickets() {
        return availableTickets;
    }

    public void setAvailableTickets(Long availableTickets) {
        this.availableTickets = availableTickets;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public long reserveTickets(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        if (availableTickets == 0) {
            throw new TicketsSoldOutException(id);
        }

        long reservedTickets = Math.min(quantity, availableTickets);
        availableTickets -= reservedTickets;

        return reservedTickets;
    }

    public void returnTickets(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        availableTickets += quantity;
    }
}
