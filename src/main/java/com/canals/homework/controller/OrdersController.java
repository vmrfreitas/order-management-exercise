package com.canals.homework.controller;

import com.canals.homework.adapter.OrderAdapter;
import com.canals.homework.controller.dto.CreateOrderRequest;
import com.canals.homework.controller.dto.CreateOrderResponse;
import com.canals.homework.event.OrderEventPublisher;
import com.canals.homework.model.Order;
import com.canals.homework.repository.OrderRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrdersController {
  private final OrderRepository orderRepository;
  private final OrderAdapter orderAdapter;
  private final OrderEventPublisher eventPublisher;

  OrdersController(
      OrderRepository orderRepository,
      OrderAdapter orderAdapter,
      OrderEventPublisher eventPublisher) {
    this.orderRepository = orderRepository;
    this.orderAdapter = orderAdapter;
    this.eventPublisher = eventPublisher;
  }

  @PostMapping("/orders")
  public ResponseEntity<CreateOrderResponse> createOrder(
      @Valid @RequestBody CreateOrderRequest request) {
    var newOrder = orderAdapter.toDomain(request);
    var savedOrder = orderRepository.save(newOrder);

    // Publish event to Kafka for async processing
    eventPublisher.publishOrderCreated(
        savedOrder.getId(), savedOrder.getCustomer(), savedOrder.getShippingAddress());

    var response =
        new CreateOrderResponse(
            savedOrder.getId(),
            savedOrder.getStatus().name(),
            "Order created. Check /orders/" + savedOrder.getId() + " for updates.");

    return ResponseEntity.accepted().body(response);
  }

  @GetMapping("/orders/{orderId}")
  public ResponseEntity<Order> getOrderStatus(@PathVariable UUID orderId) {
    var retrievedOrder =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    return ResponseEntity.ok(retrievedOrder);
  }
}
