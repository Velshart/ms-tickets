package me.mmtr.ticketservice;

import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateUpdateEventRequest;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.repository.EventRepository;
import me.mmtr.ticketservice.service.EventManagementService;
import me.mmtr.ticketservice.service.EventQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EventControllerIntegrationTests extends TicketServiceModuleIntegrationTest {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventQueryService eventQueryService;
    @Autowired
    private EventManagementService eventManagementService;

    @BeforeEach
    public void setup() {
        eventRepository.deleteAll();
    }

    private EventResponse createEvent(String eventName) {
        return eventManagementService.createEvent(new CreateUpdateEventRequest(
                eventName,
                BigDecimal.valueOf(10),
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                400
        ));
    }

    @Nested
    class GetEventByIdTests {
        private static final Long EVENT_ID = 1L;
        private static final Long USER_ID = 1L;
        private static final String USER_ROLE = "ROLE_USER";

        @Test
        void shouldGetEventByIdAndReturnOk() throws Exception {
            EventResponse eventResponse = createEvent("Event");

            mockMvc.perform(get("/api/events/{id}", eventResponse.id())
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id")
                            .value(eventResponse.id()))
                    .andExpect(jsonPath("$.name")
                            .value(eventResponse.name()));
        }

        @Test
        void shouldReturn404WhenEventNotFound() throws Exception {
            mockMvc.perform(get("/api/events/{id}", EVENT_ID)
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetAllEventsTests {
        private static final Long USER_ID = 1L;
        private static final String USER_ROLE = "ROLE_USER";

        @Test
        void shouldGetAllEventsAndReturnOk() throws Exception {
            EventResponse eventResponse1 = createEvent("Event 1");
            EventResponse eventResponse2 = createEvent("Event 2");

            mockMvc.perform(get("/api/events")
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()")
                            .value(2))
                    .andExpect(jsonPath("$[0].id")
                            .value(eventResponse1.id()))
                    .andExpect(jsonPath("$[0].name")
                            .value(eventResponse1.name()))
                    .andExpect(jsonPath("$[1].id")
                            .value(eventResponse2.id()))
                    .andExpect(jsonPath("$[1].name")
                            .value(eventResponse2.name()));
        }

        @Test
        void shouldReturnEmptyListAndOkWhenNoEventsToGet() throws Exception {
            mockMvc.perform(get("/api/events")
                            .with(withUser(USER_ID, USER_ROLE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()")
                            .value(0));
        }
    }

    @Nested
    class CreateEventTests {
        private static final CreateUpdateEventRequest REQUEST =
                new CreateUpdateEventRequest(
                        "Event",
                        BigDecimal.valueOf(10),
                        OffsetDateTime.now().plusDays(1),
                        OffsetDateTime.now().plusDays(2),
                        400
                );

        @Test
        void shouldCreateEventAndReturn201Created() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_ADMIN";

            MvcResult mvcResult = mockMvc.perform(post("/api/events")
                            .with(csrf())
                            .with(withUser(userId, userRole))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isCreated())
                    .andReturn();

            EventResponse eventResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), EventResponse.class);

            EventResponse event = eventQueryService.getEvent(eventResponse.id());
            assertEquals(REQUEST.name(), event.name());
            assertEquals(REQUEST.price(), event.price());
            assertTrue(REQUEST.startDate().isEqual(event.startDate()));
            assertTrue(REQUEST.endDate().isEqual(event.endDate()));
            assertEquals(REQUEST.availableTickets(), event.availableTickets());
        }

        @Test
        void shouldReturnForbiddenWhenRegularUserTriesToCreateEvent() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_USER";

            mockMvc.perform(post("/api/events")
                            .with(csrf())
                            .with(withUser(userId, userRole))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class UpdateEventTests {
        private static final CreateUpdateEventRequest REQUEST = new CreateUpdateEventRequest(
                "New Event",
                BigDecimal.valueOf(10),
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                400
        );

        @Test
        void shouldUpdateEventAndReturnOk() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_ADMIN";
            EventResponse eventResponse = createEvent("Event");

            mockMvc.perform(put("/api/events/{id}", eventResponse.id())
                            .with(csrf())
                            .with(withUser(userId, userRole))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isOk());

            EventResponse updatedEvent = eventQueryService.getEvent(eventResponse.id());
            assertEquals(REQUEST.name(), updatedEvent.name());
            assertEquals(REQUEST.price(), updatedEvent.price());
            assertTrue(REQUEST.startDate().isEqual(updatedEvent.startDate()));
            assertTrue(REQUEST.endDate().isEqual(updatedEvent.endDate()));
            assertEquals(
                    REQUEST.availableTickets(),
                    updatedEvent.availableTickets()
            );
        }

        @Test
        void shouldReturnNotFoundWhenEventNotFound() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_ADMIN";

            mockMvc.perform(put("/api/events/{id}", 1L)
                            .with(csrf())
                            .with(withUser(userId, userRole))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnForbiddenWhenRegularUserTriesToUpdateEvent() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_USER";
            EventResponse eventResponse = createEvent("Event");

            mockMvc.perform(put("/api/events/{id}", eventResponse.id())
                            .with(csrf())
                            .with(withUser(userId, userRole))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class UpdateEventStatusTests {
        private static final ChangeEventStatusRequest REQUEST = new ChangeEventStatusRequest(
                EventStatus.ACTIVE
        );

        @Test
        void shouldUpdateEventStatusAndReturnOk() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_ADMIN";

            EventResponse eventResponse = createEvent("Event");

            assertEquals(EventStatus.SALES_CLOSED, eventResponse.status());

            mockMvc.perform(patch("/api/events/{id}/status", eventResponse.id())
                            .with(csrf())
                            .with(withUser(userId, userRole))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isOk());

            EventResponse updatedEvent =
                    eventQueryService.getEvent(eventResponse.id());
            assertEquals(
                    eventResponse.name(),
                    updatedEvent.name()
            );
            assertEquals(
                    eventResponse.price(),
                    updatedEvent.price()
            );
            assertTrue(
                    eventResponse.startDate().isEqual(updatedEvent.startDate())
            );
            assertTrue(
                    eventResponse.endDate().isEqual(updatedEvent.endDate())
            );
            assertEquals(
                    eventResponse.availableTickets(),
                    updatedEvent.availableTickets()
            );
            assertEquals(EventStatus.ACTIVE, updatedEvent.status());
        }

        @Test
        void shouldReturnNotFoundWhenEventNotFound() throws Exception {
            Long eventId = 1L;
            Long userId = 1L;
            String userRole = "ROLE_ADMIN";

            mockMvc.perform(patch("/api/events/{id}/status", eventId)
                            .with(csrf())
                            .with(withUser(userId, userRole))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnForbiddenWhenRegularUserTriesToUpdateEventStatus() throws Exception {
            createEvent("Event");

            mockMvc.perform(patch("/api/events/{id}/status", 1L)
                            .with(csrf())
                            .with(withUser(1L, "ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(REQUEST)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class DeleteEventTests {
        @Test
        void shouldDeleteEventAndReturnNoContent() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_ADMIN";
            EventResponse eventResponse = createEvent("Event");

            mockMvc.perform(delete("/api/events/{id}", eventResponse.id())
                            .with(csrf())
                            .with(withUser(userId, userRole)))
                    .andExpect(status().isNoContent());
        }

        @Test
        void shouldReturnNotFoundWhenEventNotFound() throws Exception {
            Long eventId = 1L;
            Long userId = 1L;
            String userRole = "ROLE_ADMIN";
            mockMvc.perform(delete("/api/events/{id}", eventId)
                            .with(csrf())
                            .with(withUser(userId, userRole)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnForbiddenWhenRegularUserTriesToDeleteEvent() throws Exception {
            Long userId = 1L;
            String userRole = "ROLE_USER";
            EventResponse eventResponse = createEvent("Event");
            mockMvc.perform(delete("/api/events/{id}", eventResponse.id())
                            .with(csrf())
                            .with(withUser(userId, userRole)))
                    .andExpect(status().isForbidden());
        }
    }
}
