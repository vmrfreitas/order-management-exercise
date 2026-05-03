package com.canals.homework.controller;

import com.canals.homework.model.Order;
import com.canals.homework.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrdersController {
    private final OrderRepository repository;

    OrdersController(OrderRepository orderRepository) {
        this.repository = orderRepository;
    }

    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(@RequestBody Order order) {
        var savedOrder = repository.save(order);
        return ResponseEntity.ok("Order created with ID: " + savedOrder.getId());
    }

}
