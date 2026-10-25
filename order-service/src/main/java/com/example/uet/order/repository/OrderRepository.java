package com.example.uet.order.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.example.uet.order.model.Order;

/**
 * Simple in-memory order store.
 */
@Repository
public class OrderRepository {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    public OrderRepository() {
        // Seed orders for the users seeded in user-service (ids 1 and 2)
        save(1L, "Keyboard", new BigDecimal("49.90"));
        save(1L, "Mouse", new BigDecimal("19.90"));
        save(2L, "Monitor", new BigDecimal("199.00"));
    }

    public Collection<Order> findAll() {
        return orders.values();
    }

    public List<Order> findByUserId(Long userId) {
        return orders.values().stream()
                .filter(order -> order.userId().equals(userId))
                .toList();
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order save(Long userId, String product, BigDecimal amount) {
        long id = idSequence.incrementAndGet();
        Order order = new Order(id, userId, product, amount, Instant.now());
        orders.put(id, order);
        return order;
    }
}
