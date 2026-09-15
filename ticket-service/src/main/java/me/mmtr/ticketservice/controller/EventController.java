package me.mmtr.ticketservice.controller;

import jakarta.validation.Valid;
import me.mmtr.ticketservice.assembler.EventModelAssembler;
import me.mmtr.ticketservice.model.dto.event.ChangeEventStatusRequest;
import me.mmtr.ticketservice.model.dto.event.CreateEventRequest;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import me.mmtr.ticketservice.model.dto.event.UpdateEventRequest;
import me.mmtr.ticketservice.service.EventManagementService;
import me.mmtr.ticketservice.service.EventQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventQueryService eventQueryService;
    private final EventManagementService eventManagementService;
    private final EventModelAssembler modelAssembler;

    public EventController(
            EventQueryService eventQueryService,
            EventManagementService eventDeletionService,
            EventModelAssembler eventModelAssembler

    ) {
        this.eventQueryService = eventQueryService;
        this.eventManagementService = eventDeletionService;
        this.modelAssembler = eventModelAssembler;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<EventResponse>> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(modelAssembler.toModel(eventQueryService.getEvent(id)));
    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<EventResponse>>> getAllEvents(
            Pageable pageable,
            PagedResourcesAssembler<EventResponse> pagedResourcesAssembler
    ) {
        Page<EventResponse> eventPage = eventQueryService.getAllEvents(pageable);
        PagedModel<EntityModel<EventResponse>> pagedModel =
                pagedResourcesAssembler.toModel(eventPage, modelAssembler);

        return ResponseEntity.ok(pagedModel);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<EntityModel<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest eventRequest
    ) {
        EventResponse createdEvent =
                eventManagementService.createEvent(eventRequest);
        EntityModel<EventResponse> eventModel = modelAssembler.toModel(createdEvent);

        return ResponseEntity
                .created(eventModel.getRequiredLink("self").toUri())
                .body(eventModel);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<EntityModel<EventResponse>> updateEvent(
            @Valid @RequestBody UpdateEventRequest eventRequest,
            @PathVariable Long id
    ) {
        EventResponse updatedEvent =
                eventManagementService.updateEvent(eventRequest, id);
        return ResponseEntity.ok(modelAssembler.toModel(updatedEvent));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<EntityModel<EventResponse>> updateEventStatus(
            @Valid @RequestBody ChangeEventStatusRequest changeStatusRequest,
            @PathVariable Long id
    ) {
        EventResponse updatedEvent =
                eventManagementService.changeEventStatus(changeStatusRequest, id);
        return ResponseEntity.ok(modelAssembler.toModel(updatedEvent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventManagementService.deleteEventAndOrders(id);
        return ResponseEntity.noContent().build();
    }
}
