package me.zinch.itmo.mts.web.eis;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.service.eis.ExternalBillingService;
import me.zinch.itmo.mts.web.order.dto.OrderApiResponse;
import me.zinch.itmo.mts.web.order.mapper.OrderMapper;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderEisController {

    private final ExternalBillingService externalBillingService;
    private final OrderMapper orderMapper;

    @PostMapping("/{orderId}/eis/provision")
    @PreAuthorize("hasAuthority('EIS_PROVISION')")
    public OrderApiResponse provisionPaidOrder(@PathVariable UUID orderId) {
        return orderMapper.toResponse(externalBillingService.provisionPaidOrder(orderId));
    }
}
