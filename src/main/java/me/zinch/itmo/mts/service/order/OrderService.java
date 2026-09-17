package me.zinch.itmo.mts.service.order;

import me.zinch.itmo.mts.domain.entity.Order;
import me.zinch.itmo.mts.domain.enums.OrderStatus;
import me.zinch.itmo.mts.service.order.dto.CreateOrderRequest;
import me.zinch.itmo.mts.service.order.message.event.OrderPlaced;
import me.zinch.itmo.mts.service.order.message.event.PaymentLinkCreated;
import me.zinch.itmo.mts.service.order.message.event.PaymentLinkRequested;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.validation.Valid;

import java.util.UUID;

public interface OrderService {

    Order createOrder(@Valid CreateOrderRequest request);

    Order getOrderForUser(UUID userId, UUID orderId);

    Order updateOrder(UUID orderId, UUID seniorManagerId, @Valid CreateOrderRequest request);

    void deleteOrder(UUID orderId);

    Page<Order> getOrdersForUser(UUID userId, Pageable pageable);

    Order assignManager(UUID orderId, UUID managerId);

    Order changeStatus(UUID orderId, UUID managerId, OrderStatus newStatus);

    PaymentLinkCreated createPaymentLink(PaymentLinkRequested request);

    OrderPlaced placeOrder(PaymentLinkCreated paymentLink);
}
