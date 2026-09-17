package me.mmtr.ticketservice.controller;

import jakarta.validation.Valid;
import me.mmtr.ticketservice.assembler.OrderModelAssembler;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import me.mmtr.ticketservice.model.dto.order.OrderResponse;
import me.mmtr.ticketservice.model.dto.order.OrderStatusResponse;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderModelAssembler orderModelAssembler;

    public OrderController(
            OrderService orderService,
            OrderModelAssembler orderModelAssembler
    ) {
        this.orderService = orderService;
        this.orderModelAssembler = orderModelAssembler;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<EntityModel<OrderResponse>> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        OrderResponse order = orderService.getOrder(orderId, authenticatedUser.id());
        return ResponseEntity.ok(orderModelAssembler.toModel(order));
    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<OrderResponse>>> getAllOrdersForUser(
            Pageable pageable,
            PagedResourcesAssembler<OrderResponse> pagedResourcesAssembler,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        Page<OrderResponse> orderPage = orderService.getAllOrdersByUserId(authenticatedUser.id(), pageable);
        PagedModel<EntityModel<OrderResponse>> pagedModel =
                pagedResourcesAssembler.toModel(orderPage, orderModelAssembler);

        return ResponseEntity.ok(pagedModel);
    }

    @PostMapping
    public ResponseEntity<EntityModel<OrderResponse>> submitOrder(
            @Valid @RequestBody BuyTicketsRequest buyTicketsRequest,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        OrderResponse submittedOrder = orderService.submitOrder(
                buyTicketsRequest,
                authenticatedUser.id()
        );
        EntityModel<OrderResponse> orderModel =
                orderModelAssembler.toModel(submittedOrder);

        return ResponseEntity.accepted()
                .location(orderModel.getRequiredLink("self").toUri())
                .body(orderModel);
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                orderService.getOrderStatus(orderId, authenticatedUser.id())
        );
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrderAsUser(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        orderService.deleteOrderAsUser(orderId, authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/{orderId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteOrderAsAdmin(
            @PathVariable Long orderId
    ) {
        orderService.deleteOrderAsAdmin(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<OrderResponse>>> getUserOrders(
            Pageable pageable,
            @PathVariable Long userId,
            PagedResourcesAssembler<OrderResponse> pagedResourcesAssembler
    ) {
        Page<OrderResponse> orderPage = orderService.getAllOrdersByUserId(userId, pageable);
        PagedModel<EntityModel<OrderResponse>> pagedModel =
                pagedResourcesAssembler.toModel(orderPage, orderModelAssembler);
        return ResponseEntity.ok(pagedModel);
    }
}