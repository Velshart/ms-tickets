package me.mmtr.ticketservice.controller;

import me.mmtr.ticketservice.assembler.EventModelAssembler;
import me.mmtr.ticketservice.configuration.filter.RateLimitingFilter;
import me.mmtr.ticketservice.model.EventStatus;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateEventRequest;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.dto.event.UpdateEventRequest;
import me.mmtr.ticketservice.model.exception.EventAlreadyRegisteredException;
import me.mmtr.ticketservice.model.exception.EventNotFoundException;
import me.mmtr.ticketservice.service.EventManagementService;
import me.mmtr.ticketservice.service.EventQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitingFilter.class
        ))
@Import(EventModelAssembler.class)
public class EventControllerTests {
    private static final Long EVENT_ID = 1L;
    private static final String EVENT_NAME = "Event";
    private static final BigDecimal EVENT_PRICE = BigDecimal.valueOf(100.0);
    private static final long AVAILABLE_TICKETS = 250L;

    @MockitoBean
    private EventQueryService eventQueryService;

    @MockitoBean
    private EventManagementService eventManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private EventResponse eventResponse;
    private CreateEventRequest createEventRequest;
    private UserDetails adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new AuthenticatedUser(
                1L,
                "admin",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        eventResponse = new EventResponse(
                EVENT_ID,
                EVENT_NAME,
                EVENT_PRICE,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2),
                EventStatus.ACTIVE,
                AVAILABLE_TICKETS
        );

        createEventRequest = new CreateEventRequest(
                EVENT_NAME,
                EVENT_PRICE,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                AVAILABLE_TICKETS
        );
    }

    @Nested
    class GetEventByIdTests {
        @Test
        void shouldReturnEventByIdWithOkStatusAndHateoasLinks() throws Exception {
            when(eventQueryService.getEvent(EVENT_ID)).thenReturn(eventResponse);

            mockMvc.perform(get("/api/events/{id}", EVENT_ID)
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EVENT_ID))
                    .andExpect(jsonPath("$.name").value(EVENT_NAME))
                    .andExpect(jsonPath("$.availableTickets").value(AVAILABLE_TICKETS))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.events.href").exists())
                    .andExpect(jsonPath("$._links.update.href").exists())
                    .andExpect(jsonPath("$._links.update-status.href").exists())
                    .andExpect(jsonPath("$._links.delete.href").exists());

            verify(eventQueryService).getEvent(EVENT_ID);
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
            when(eventQueryService.getEvent(EVENT_ID))
                    .thenThrow(new EventNotFoundException(EVENT_ID));

            mockMvc.perform(get("/api/events/{id}", EVENT_ID)
                            .with(user(adminUser)))
                    .andExpect(status().isNotFound());

            verify(eventQueryService).getEvent(EVENT_ID);
        }
    }

    @Nested
    class GetAllEventsTests {
        @Test
        void shouldReturnPagedEventsWithHateoasLinks() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<EventResponse> page = new PageImpl<>(List.of(eventResponse), pageable, 1);

            when(eventQueryService.getAllEvents(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/events")
                            .param("page", "0")
                            .param("size", "10")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.events[0].id").value(EVENT_ID))
                    .andExpect(jsonPath("$._embedded.events[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$.page.totalElements").value(1));

            verify(eventQueryService).getAllEvents(any(Pageable.class));
        }
    }

    @Nested
    class CreateEventTests {
        @Test
        void shouldCreateEventAndReturnLocationHeader() throws Exception {
            when(eventManagementService.createEvent(any(CreateEventRequest.class)))
                    .thenReturn(eventResponse);

            mockMvc.perform(post("/api/events")
                            .with(csrf())
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createEventRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").value(EVENT_ID))
                    .andExpect(jsonPath("$.name").value(EVENT_NAME))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(eventManagementService).createEvent(any(CreateEventRequest.class));
        }

        @Test
        void shouldReturnConflictWhenEventAlreadyExists() throws Exception {
            when(eventManagementService.createEvent(any(CreateEventRequest.class)))
                    .thenThrow(new EventAlreadyRegisteredException(createEventRequest.name()));

            mockMvc.perform(post("/api/events")
                            .with(csrf())
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createEventRequest)))
                    .andExpect(status().isConflict());

            verify(eventManagementService).createEvent(any(CreateEventRequest.class));
        }
    }

    @Nested
    class UpdateEventTests {
        @Test
        void shouldUpdateEvent() throws Exception {
            when(eventManagementService.updateEvent(any(UpdateEventRequest.class), eq(EVENT_ID)))
                    .thenReturn(eventResponse);

            mockMvc.perform(put("/api/events/{id}", EVENT_ID)
                            .with(csrf())
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createEventRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EVENT_ID))
                    .andExpect(jsonPath("$.name").value(EVENT_NAME))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(eventManagementService).updateEvent(any(UpdateEventRequest.class), eq(EVENT_ID));
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
            when(eventManagementService.updateEvent(any(UpdateEventRequest.class), eq(EVENT_ID)))
                    .thenThrow(new EventNotFoundException(EVENT_ID));

            mockMvc.perform(put("/api/events/{id}", EVENT_ID)
                            .with(csrf())
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createEventRequest)))
                    .andExpect(status().isNotFound());

            verify(eventManagementService).updateEvent(any(UpdateEventRequest.class), eq(EVENT_ID));
        }
    }

    @Nested
    class ChangeEventStatusTests {
        @Test
        void shouldUpdateEventStatus() throws Exception {
            ChangeEventStatusRequest statusRequest = new ChangeEventStatusRequest(EventStatus.CANCELLED);

            when(eventManagementService.changeEventStatus(any(ChangeEventStatusRequest.class), eq(EVENT_ID)))
                    .thenReturn(eventResponse);

            mockMvc.perform(patch("/api/events/{id}/status", EVENT_ID)
                            .with(csrf())
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EVENT_ID))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(eventManagementService).changeEventStatus(any(ChangeEventStatusRequest.class), eq(EVENT_ID));
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
            ChangeEventStatusRequest statusRequest = new ChangeEventStatusRequest(EventStatus.CANCELLED);

            when(eventManagementService.changeEventStatus(any(ChangeEventStatusRequest.class), eq(EVENT_ID)))
                    .thenThrow(new EventNotFoundException(EVENT_ID));

            mockMvc.perform(patch("/api/events/{id}/status", EVENT_ID)
                            .with(csrf())
                            .with(user(adminUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusRequest)))
                    .andExpect(status().isNotFound());

            verify(eventManagementService).changeEventStatus(any(ChangeEventStatusRequest.class), eq(EVENT_ID));
        }
    }

    @Nested
    class DeleteEventTests {
        @Test
        void shouldDeleteEvent() throws Exception {
            mockMvc.perform(delete("/api/events/{id}", EVENT_ID)
                            .with(csrf())
                            .with(user(adminUser)))
                    .andExpect(status().isNoContent());

            verify(eventManagementService).deleteEventAndOrders(EVENT_ID);
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
            doThrow(new EventNotFoundException(EVENT_ID))
                    .when(eventManagementService).deleteEventAndOrders(EVENT_ID);

            mockMvc.perform(delete("/api/events/{id}", EVENT_ID)
                            .with(csrf())
                            .with(user(adminUser)))
                    .andExpect(status().isNotFound());
        }
    }
}