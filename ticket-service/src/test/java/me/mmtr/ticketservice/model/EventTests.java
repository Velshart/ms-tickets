package me.mmtr.ticketservice.model;

import me.mmtr.ticketservice.model.exception.TicketsSoldOutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EventTests {
    private Event event;

    @BeforeEach
    public void setup() {
        event = new Event(
                "Event",
                BigDecimal.valueOf(300),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(1),
                10
        );
    }

    @Nested
    class ReserveTicketsTests {
        @Test
        void shouldThrowIllegalArgumentExceptionWhenQuantityIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> event.reserveTickets(0)
            );
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenQuantityIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> event.reserveTickets(-1)
            );
        }

        @Test
        void shouldThrowTicketsSoldOutExceptionWhenNoTicketsLeft() {
            event.setAvailableTickets(0L);
            assertThrows(
                    TicketsSoldOutException.class,
                    () -> event.reserveTickets(1)
            );
        }

        @Test
        void shouldReserveAsManyTicketsAsPossibleWhenQuantityIsBiggerThanNumberOfAvailableTickets() {
            long reservedTickets = event.reserveTickets(15);
            assertEquals(10, reservedTickets);
            assertEquals(0, event.getAvailableTickets());
        }
    }

    @Nested
    class ReturnTicketsTests {
        @Test
        void shouldThrowIllegalArgumentExceptionWhenQuantityIsZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> event.returnTickets(0)
            );
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenQuantityIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> event.returnTickets(-1)
            );
        }

        @Test
        void shouldIncreaseAmountOfAvailableTickets() {
            event.returnTickets(1);
            assertEquals(11, event.getAvailableTickets());
        }
    }
}
