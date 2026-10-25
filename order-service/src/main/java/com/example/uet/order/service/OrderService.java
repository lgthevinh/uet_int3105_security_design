package com.example.uet.order.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import com.example.uet.order.dto.CreateOrderRequest;
import com.example.uet.order.exception.NotFoundException;
import com.example.uet.order.model.Order;
import com.example.uet.order.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Lists orders, optionally filtered by user.
     * The userId filter is what the gateway /dashboard aggregation will call.
     */
    public Collection<Order> getAll(Long userId) {
        return userId == null ? orderRepository.findAll() : orderRepository.findByUserId(userId);
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }

    public Order create(CreateOrderRequest request) {
        return orderRepository.save(request.userId(), request.product(), request.amount());
    }
}
