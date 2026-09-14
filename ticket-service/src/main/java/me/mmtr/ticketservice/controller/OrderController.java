package me.mmtr.ticketservice.controller;

import jakarta.validation.Valid;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import me.mmtr.ticketservice.model.dto.ticket.BuyTicketsRequest;
import me.mmtr.ticketservice.model.dto.order.OrderResponse;
import me.mmtr.ticketservice.model.dto.order.OrderStatusResponse;
import me.mmtr.ticketservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                orderService.getOrder(
                        orderId,
                        authenticatedUser.id()
                ));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrdersForUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(
                orderService.getAllOrdersByUserId(
                        authenticatedUser.id()
                )
        );
    }

    @PostMapping
    public ResponseEntity<OrderResponse> submitOrder(
            @Valid @RequestBody BuyTicketsRequest buyTicketsRequest,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.accepted().body(
                orderService.submitOrder(
                        buyTicketsRequest,
                        authenticatedUser.id()
                )
        );
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
}
