package me.zinch.itmo.mts.web.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentSucceededWebhookRequest(@NotBlank String yooKassaPaymentId) {
}
