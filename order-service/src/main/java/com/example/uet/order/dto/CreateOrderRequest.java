package com.example.uet.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotNull Long userId,
        @NotBlank String product,
        @NotNull @Positive BigDecimal amount) {
}
