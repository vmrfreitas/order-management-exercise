package com.canals.homework.controller;

import com.canals.homework.controller.dto.CreateOrderRequest;
import com.canals.homework.model.Order;
import com.canals.homework.model.OrderItem;
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
  public ResponseEntity<String> createOrder(@RequestBody CreateOrderRequest request) {
    Order order = new Order();
    order.setCustomer(request.getCustomer());
    order.setShippingAddress(request.getShippingAddress());

    var orderItems =
        request.getItems().stream()
            .map(
                itemRequest -> {
                  OrderItem item = new OrderItem();
                  item.setOrder(order);
                  item.setQuantity(itemRequest.getQuantity());
                  // Product will be set separately by repository or service
                  return item;
                })
            .toList();

    order.setItems(orderItems);
    var savedOrder = repository.save(order);
    return ResponseEntity.ok("Order created with ID: " + savedOrder.getId());
  }
}
