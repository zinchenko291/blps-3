package me.zinch.itmo.mts.service.order.message.event;

import java.util.UUID;

public record PaymentLinkRequested(UUID orderId, String idempotenceKey) {
}
