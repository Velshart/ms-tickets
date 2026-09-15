package me.mmtr.ticketservice.assembler;

import me.mmtr.ticketservice.controller.EventController;
import me.mmtr.ticketservice.model.dto.event.EventResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class EventModelAssembler implements RepresentationModelAssembler<EventResponse, EntityModel<EventResponse>> {

    @Override
    public EntityModel<EventResponse> toModel(EventResponse event) {
        return EntityModel.of(
                event,
                linkTo(methodOn(EventController.class).getEventById(event.id())).withSelfRel(),
                linkTo(methodOn(EventController.class).getAllEvents(Pageable.unpaged(), null)).withRel("events"),
                linkTo(methodOn(EventController.class).updateEvent(null, event.id())).withRel("update"),
                linkTo(methodOn(EventController.class).updateEventStatus(null, event.id())).withRel("update-status"),
                linkTo(methodOn(EventController.class).deleteEvent(event.id())).withRel("delete")
        );
    }
}