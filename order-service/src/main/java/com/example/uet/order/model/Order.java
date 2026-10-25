package com.example.uet.order.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(Long id, Long userId, String product, BigDecimal amount, Instant createdAt) {
}
