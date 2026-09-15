package me.mmtr.ticketservice.assembler;

import me.mmtr.ticketservice.controller.EventController;
import me.mmtr.ticketservice.controller.OrderController;
import me.mmtr.ticketservice.model.dto.order.OrderResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class OrderModelAssembler implements RepresentationModelAssembler<OrderResponse, EntityModel<OrderResponse>> {

    @Override
    @NonNull
    public EntityModel<OrderResponse> toModel(@NonNull OrderResponse order) {
        return EntityModel.of(
                order,
                linkTo(methodOn(OrderController.class).getOrderById(order.id(), null)).withSelfRel(),
                linkTo(methodOn(OrderController.class).getAllOrdersForUser(Pageable.unpaged(), null, null)).withRel("user-orders"),
                linkTo(methodOn(OrderController.class).getOrderStatus(order.id(), null)).withRel("status"),
                linkTo(methodOn(OrderController.class).deleteOrderAsUser(order.id(), null)).withRel("cancel-user"),
                linkTo(methodOn(EventController.class).getEventById(order.eventId())).withRel("event")
        );
    }
}
