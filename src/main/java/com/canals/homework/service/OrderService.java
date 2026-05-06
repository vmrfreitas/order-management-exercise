package com.canals.homework.service;

import com.canals.homework.controller.LocationClient;
import com.canals.homework.controller.PaymentClient;
import com.canals.homework.model.Order;
import com.canals.homework.model.OrderStatus;
import com.canals.homework.model.Warehouse;
import com.canals.homework.repository.InventoryItemRepository;
import com.canals.homework.repository.OrderRepository;
import com.canals.homework.repository.WarehouseRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final WarehouseRepository warehouseRepository;
  private final InventoryItemRepository inventoryItemRepository;
  private final LocationClient locationClient;
  private final PaymentClient paymentClient;

  public OrderService(
      OrderRepository orderRepository,
      WarehouseRepository warehouseRepository,
      InventoryItemRepository inventoryItemRepository,
      LocationClient locationClient,
      PaymentClient paymentClient) {
    this.orderRepository = orderRepository;
    this.warehouseRepository = warehouseRepository;
    this.inventoryItemRepository = inventoryItemRepository;
    this.locationClient = locationClient;
    this.paymentClient = paymentClient;
  }

  @Transactional
  public void fulfillOrder(UUID orderId) {
    try {
      var foundOrder = findOrder(orderId);
      var warehousesWithAllItems = warehouseRepository.findWarehousesWithAllItems(orderId);

      if (warehousesWithAllItems.isEmpty()) {
        logger.warn("No warehouse with all items found for order: {}", orderId);
        markOrderAsFailed(foundOrder);
        return;
      }

      var selectedWarehouse =
          selectClosestWarehouse(warehousesWithAllItems, foundOrder.getShippingAddress());
      if (selectedWarehouse == null) {
        logger.error("Failed to select a warehouse for order: {}", orderId);
        markOrderAsFailed(foundOrder);
        return;
      }

      if (!decreaseInventory(selectedWarehouse, foundOrder)) {
        markOrderAsFailed(foundOrder);
        return;
      }

      if (!processPayment(foundOrder)) {
        markOrderAsFailed(foundOrder);
        return;
      }

      markOrderAsFulfilled(foundOrder, selectedWarehouse);

    } catch (Exception e) {
      logger.error("Error fulfilling order {}: {}", orderId, e.getMessage(), e);
      markOrderAsFailedSafely(orderId);
    }
  }

  private Order findOrder(UUID orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
  }

  private Warehouse selectClosestWarehouse(
      java.util.List<Warehouse> warehouses, String shippingAddress) {
    var closestWarehouse = (Warehouse) null;
    var minimumDistance = Double.MAX_VALUE;

    for (var currentWarehouse : warehouses) {
      var distanceToShippingLocation =
          locationClient.getDistance(currentWarehouse.getAddress(), shippingAddress);
      if (distanceToShippingLocation < minimumDistance) {
        minimumDistance = distanceToShippingLocation;
        closestWarehouse = currentWarehouse;
      }
    }

    return closestWarehouse;
  }

  private boolean decreaseInventory(Warehouse warehouse, Order order) {
    for (var orderItem : order.getItems()) {
      var inventoryUpdateCount =
          inventoryItemRepository.decreaseQuantity(
              warehouse.getId(), orderItem.getProduct().getProductId(), orderItem.getQuantity());

      if (inventoryUpdateCount == 0) {
        logger.error(
            "Failed to deduct inventory for product: {} in warehouse: {}",
            orderItem.getProduct().getProductId(),
            warehouse.getId());
        return false;
      }

      inventoryItemRepository.deleteIfEmpty(
          warehouse.getId(), orderItem.getProduct().getProductId());
    }

    return true;
  }

  private boolean processPayment(Order order) {
    var totalAmount =
        order.getItems().stream()
            .mapToDouble(item -> item.getQuantity() * item.getProduct().getPrice())
            .sum();
    var description = "Payment for order " + order.getId();

    var paymentSuccess =
        paymentClient.processPayment(order.getCreditCardNumber(), totalAmount, description);

    if (!paymentSuccess) {
      logger.error("Payment failed for order: {}", order.getId());
    }

    return paymentSuccess;
  }

  private void markOrderAsFulfilled(Order order, Warehouse warehouse) {
    order.setStatus(OrderStatus.FULFILLED);
    orderRepository.save(order);
    logger.info("Order {} fulfilled from warehouse {}", order.getId(), warehouse.getId());
  }

  private void markOrderAsFailed(Order order) {
    order.setStatus(OrderStatus.FAILED);
    orderRepository.save(order);
  }

  private void markOrderAsFailedSafely(UUID orderId) {
    try {
      var possibleOrder = orderRepository.findById(orderId).orElse(null);
      if (possibleOrder != null) {
        possibleOrder.setStatus(OrderStatus.FAILED);
        orderRepository.save(possibleOrder);
      }
    } catch (Exception innerException) {
      logger.error("Failed to update order status to FAILED", innerException);
    }
  }
}
