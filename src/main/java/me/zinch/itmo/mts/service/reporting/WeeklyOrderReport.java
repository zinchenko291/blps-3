package me.zinch.itmo.mts.service.reporting;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WeeklyOrderReport(
        OffsetDateTime periodFrom,
        OffsetDateTime periodTo,
        long totalOrders,
        long successfulOrders,
        BigDecimal successfulOrdersAmount) {
}
