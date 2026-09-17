package me.zinch.itmo.mts.service.payment;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.domain.entity.Order;
import me.zinch.itmo.mts.domain.enums.OrderStatus;
import me.zinch.itmo.mts.domain.enums.YooKassaPaymentStatus;
import me.zinch.itmo.mts.domain.payment.Payment;
import me.zinch.itmo.mts.repository.OrderRepository;
import me.zinch.itmo.mts.repository.payment.PaymentRepository;
import me.zinch.itmo.mts.service.ServiceException;
import me.zinch.itmo.mts.service.eis.ExternalBillingService;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TransactionTemplate jtaTransactionTemplate;
    private final ExternalBillingService externalBillingService;

    public Order markPaymentSucceeded(String yooKassaPaymentId) {
        Order paidOrder = jtaTransactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByYooKassaPaymentId(yooKassaPaymentId)
                    .orElseThrow(() -> new ServiceException("Платёж YooKassa не найден: " + yooKassaPaymentId));
            if (payment.getStatus() == YooKassaPaymentStatus.CANCELED) {
                throw new ServiceException("Отменённый платёж нельзя отметить как успешный: " + yooKassaPaymentId);
            }
            payment.setStatus(YooKassaPaymentStatus.SUCCEEDED);
            paymentRepository.save(payment);

            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + payment.getOrderId()));
            if (order.getStatus() == OrderStatus.PAID) {
                return order;
            }
            if (order.getStatus() != OrderStatus.PLACED) {
                throw new ServiceException("Оплатить можно только заказ в статусе PLACED");
            }
            order.setStatus(OrderStatus.PAID);
            order.setUpdatedAt(OffsetDateTime.now());
            return orderRepository.saveAndFlush(order);
        });
        if (paidOrder.getStatus() == OrderStatus.SERVICES_ACTIVATED) {
            return paidOrder;
        }
        return externalBillingService.provisionPaidOrder(paidOrder.getId());
    }
}
