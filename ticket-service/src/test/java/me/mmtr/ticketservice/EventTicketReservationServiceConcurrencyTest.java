package me.mmtr.ticketservice;

import me.mmtr.ticketservice.model.Event;
import me.mmtr.ticketservice.repository.EventRepository;
import me.mmtr.ticketservice.service.EventTicketReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventTicketReservationServiceConcurrencyTest
        extends TicketServiceModuleIntegrationTest {

    @Autowired
    private EventTicketReservationService reservationService;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldHandleConcurrentReservationsUsingOptimisticLocking()
            throws InterruptedException, ExecutionException {

        Event event = new Event(
                "Test Event",
                BigDecimal.TEN,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                100
        );

        event = eventRepository.saveAndFlush(event);

        Long eventId = event.getId();

        int numberOfRequests = 20;

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(numberOfRequests)
        ) {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            List<Future<Long>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfRequests; i++) {
                futures.add(executor.submit(() -> {
                    countDownLatch.await();

                    return reservationService.reserveTickets(eventId, 1);
                }));
            }

            countDownLatch.countDown();

            int successfulReservations = 0;

            for (Future<Long> future : futures) {
                Long reserved = future.get();

                assertEquals(1L, reserved);
                successfulReservations++;
            }

            assertEquals(
                    numberOfRequests,
                    successfulReservations
            );

            Event result = eventRepository.findById(eventId)
                    .orElseThrow();

            assertEquals(
                    80L,
                    result.getAvailableTickets()
            );
        }
    }
}