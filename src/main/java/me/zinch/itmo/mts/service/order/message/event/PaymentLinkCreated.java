package me.zinch.itmo.mts.service.order.message.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentLinkCreated(
        UUID orderId,
        String yooKassaPaymentId,
        String idempotenceKey,
        BigDecimal amount,
        String currency,
        String rawStatus,
        String confirmationUrl) {
}
