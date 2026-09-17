package me.zinch.itmo.mts.service.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.domain.entity.OrderItem;
import me.zinch.itmo.mts.domain.enums.OrderStatus;
import me.zinch.itmo.mts.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final OrderRepository orderRepository;
    private final TransactionTemplate jtaTransactionTemplate;

    public WeeklyOrderReport buildWeeklyOrderReport() {
        OffsetDateTime periodTo = OffsetDateTime.now();
        OffsetDateTime periodFrom = periodTo.minusWeeks(1);
        return jtaTransactionTemplate.execute(status -> createReport(periodFrom, periodTo));
    }

    private WeeklyOrderReport createReport(OffsetDateTime periodFrom, OffsetDateTime periodTo) {
        long totalOrders = orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(periodFrom, periodTo);
        var successfulOrders = orderRepository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatus(
                periodFrom, periodTo, OrderStatus.PAID);
        BigDecimal amount = successfulOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(this::itemAmount)
                .reduce(BigDecimal.ZERO, (num1, num2) -> num1.add(num2))
                .setScale(2, RoundingMode.HALF_UP);

        return new WeeklyOrderReport(periodFrom, periodTo, totalOrders,
                successfulOrders.size(), amount);
    }

    private BigDecimal itemAmount(OrderItem item) {
        return item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
