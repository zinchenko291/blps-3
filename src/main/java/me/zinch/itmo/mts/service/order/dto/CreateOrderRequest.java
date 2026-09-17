package me.zinch.itmo.mts.service.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerName,
        @NotBlank String phoneNumber,
        @NotBlank @Email String email,
        @NotEmpty List<@NotNull @Valid OrderItemRequest> items
) {
}
