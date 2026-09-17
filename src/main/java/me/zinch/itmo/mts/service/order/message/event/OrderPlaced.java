package me.zinch.itmo.mts.service.order.message.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlaced(UUID orderId, String paymentUrl, BigDecimal totalAmount) {
}
