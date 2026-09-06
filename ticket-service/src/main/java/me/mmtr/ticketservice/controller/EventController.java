package me.mmtr.ticketservice.controller;

import jakarta.validation.Valid;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateUpdateEventRequest;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.service.EventManagementService;
import me.mmtr.ticketservice.service.EventQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventQueryService eventQueryService;
    private final EventManagementService eventManagementService;

    public EventController(
            EventQueryService eventQueryService,
            EventManagementService eventDeletionService
    ) {
        this.eventQueryService = eventQueryService;
        this.eventManagementService = eventDeletionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventQueryService.getEvent(id));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventQueryService.getAllEvents());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateUpdateEventRequest eventRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventManagementService.createEvent(eventRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @Valid @RequestBody CreateUpdateEventRequest eventRequest,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                eventManagementService.updateEvent(eventRequest, id)
        );
    }

    @PatchMapping("{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<EventResponse> updateEventStatus(
            @Valid @RequestBody ChangeEventStatusRequest changeStatusRequest,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                eventManagementService.changeEventStatus(changeStatusRequest, id)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventManagementService.deleteEventAndOrders(id);
        return ResponseEntity.noContent().build();
    }
}
