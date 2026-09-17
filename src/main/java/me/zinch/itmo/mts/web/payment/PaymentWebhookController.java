package me.zinch.itmo.mts.web.payment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.service.payment.PaymentWebhookService;
import me.zinch.itmo.mts.web.order.dto.OrderApiResponse;
import me.zinch.itmo.mts.web.order.mapper.OrderMapper;
import me.zinch.itmo.mts.web.payment.dto.PaymentSucceededWebhookRequest;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;
    private final OrderMapper orderMapper;

    @PostMapping("/succeeded")
    public OrderApiResponse paymentSucceeded(@Valid @RequestBody PaymentSucceededWebhookRequest request) {
        return orderMapper.toResponse(paymentWebhookService.markPaymentSucceeded(request.yooKassaPaymentId()));
    }
}
