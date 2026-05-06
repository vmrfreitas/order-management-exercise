package com.canals.homework.service;

import com.canals.homework.controller.LocationClient;
import com.canals.homework.controller.PaymentClient;
import com.canals.homework.model.Order;
import com.canals.homework.model.OrderStatus;
import com.canals.homework.model.Warehouse;
import com.canals.homework.repository.InventoryItemRepository;
import com.canals.homework.repository.OrderRepository;
import com.canals.homework.repository.WarehouseRepository;
import java.math.BigDecimal;
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
      Order foundOrder = findOrder(orderId);
      var warehousesWithAllItems = warehouseRepository.findWarehousesWithAllItems(orderId);

      if (warehousesWithAllItems.isEmpty()) {
        logger.warn("No warehouse with all items found for order: {}", orderId);
        markOrderAsFailed(foundOrder);
        return;
      }

      Warehouse selectedWarehouse =
          selectClosestWarehouse(warehousesWithAllItems, foundOrder.getShippingAddress());
      if (selectedWarehouse == null) {
        logger.error("Failed to select a warehouse for order: {}", orderId);
        markOrderAsFailed(foundOrder);
        return;
      }

      // Both inventory deduction and payment are within @Transactional.
      // If either fails (throws), the entire transaction rolls back — no partial deductions.
      decreaseInventory(selectedWarehouse, foundOrder);
      processPayment(foundOrder);

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
    Warehouse closestWarehouse = null;
    double minimumDistance = Double.MAX_VALUE;

    for (Warehouse currentWarehouse : warehouses) {
      double distanceToShippingLocation =
          locationClient.getDistance(currentWarehouse.getAddress(), shippingAddress);
      if (distanceToShippingLocation < minimumDistance) {
        minimumDistance = distanceToShippingLocation;
        closestWarehouse = currentWarehouse;
      }
    }

    return closestWarehouse;
  }

  /**
   * Deducts inventory for all items in the order from the selected warehouse. Throws a
   * RuntimeException if any deduction fails, which causes @Transactional to roll back all
   * previously-deducted items in this transaction.
   */
  private void decreaseInventory(Warehouse warehouse, Order order) {
    for (var orderItem : order.getItems()) {
      int inventoryUpdateCount =
          inventoryItemRepository.decreaseQuantity(
              warehouse.getId(), orderItem.getProduct().getProductId(), orderItem.getQuantity());

      if (inventoryUpdateCount == 0) {
        throw new RuntimeException(
            String.format(
                "Insufficient inventory for product %s in warehouse %s",
                orderItem.getProduct().getProductId(), warehouse.getId()));
      }

      inventoryItemRepository.deleteIfEmpty(
          warehouse.getId(), orderItem.getProduct().getProductId());
    }
  }

  /**
   * Processes payment for the order. Throws a RuntimeException on failure, which causes
   * the @Transactional to roll back inventory deductions.
   */
  private void processPayment(Order order) {
    BigDecimal totalAmount =
        order.getItems().stream()
            .map(
                item ->
                    item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    String description = "Payment for order " + order.getId();

    boolean paymentSuccess =
        paymentClient.processPayment(order.getCreditCardNumber(), totalAmount, description);

    if (!paymentSuccess) {
      throw new RuntimeException("Payment failed for order: " + order.getId());
    }
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
