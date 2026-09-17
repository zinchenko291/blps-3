package me.zinch.itmo.mts.web.order;

import jakarta.validation.Valid;
import me.zinch.itmo.mts.domain.entity.Order;
import me.zinch.itmo.mts.service.order.OrderService;
import me.zinch.itmo.mts.security.SecurityAccount;
import me.zinch.itmo.mts.service.order.dto.CreateOrderRequest;
import me.zinch.itmo.mts.web.order.dto.AssignManagerApiRequest;
import me.zinch.itmo.mts.web.order.dto.ChangeOrderStatusApiRequest;
import me.zinch.itmo.mts.web.order.dto.CreateOrderApiRequest;
import me.zinch.itmo.mts.web.order.dto.OrderApiResponse;
import me.zinch.itmo.mts.web.order.mapper.OrderMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(
            OrderService orderService,
            OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @GetMapping
    public Page<OrderApiResponse> getOrders(
            @AuthenticationPrincipal SecurityAccount account,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.getOrdersForUser(account.id(), pageable).map(orderMapper::toResponse);
    }

    @GetMapping("/{orderId}")
    public OrderApiResponse getOrderById(@PathVariable UUID orderId, @AuthenticationPrincipal SecurityAccount account) {
        return orderMapper.toResponse(orderService.getOrderForUser(account.id(), orderId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderApiResponse> createOrder(
            @Valid @RequestBody CreateOrderApiRequest request) {
        Order createdOrder = orderService.createOrder(new CreateOrderRequest(
                request.getCustomerName(),
                request.getPhoneNumber(),
                request.getEmail(),
                orderMapper.toServiceItems(request.getItems())));
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponse(createdOrder));
    }

    @PutMapping(value = "/{orderId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderApiResponse updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateOrderApiRequest request,
            @AuthenticationPrincipal SecurityAccount account) {
        Order updated = orderService.updateOrder(orderId, account.id(), new CreateOrderRequest(
                request.getCustomerName(),
                request.getPhoneNumber(),
                request.getEmail(),
                orderMapper.toServiceItems(request.getItems())));
        return orderMapper.toResponse(updated);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{orderId}/manager")
    public OrderApiResponse assignManager(
            @PathVariable UUID orderId,
            @Valid @RequestBody AssignManagerApiRequest request) {
        Order updated = orderService.assignManager(orderId, request.getManagerId());
        return orderMapper.toResponse(updated);
    }

    @PatchMapping("/{orderId}/status")
    public OrderApiResponse changeStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody ChangeOrderStatusApiRequest request,
            @AuthenticationPrincipal SecurityAccount account) {
        Order updated = orderService.changeStatus(orderId, account.id(), request.getStatus());
        return orderMapper.toResponse(updated);
    }

}
