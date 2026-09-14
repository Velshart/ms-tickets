package me.mmtr.ticketservice.model.dto.order;

import me.mmtr.ticketservice.model.Order;
import me.mmtr.ticketservice.model.OrderStatus;

public record OrderStatusResponse(Long orderId, OrderStatus orderStatus) {
    public static OrderStatusResponse from(Order order) {
        return new OrderStatusResponse(
                order.getId(),
                order.getOrderStatus()
        );
    }
}
