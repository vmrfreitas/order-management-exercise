package com.canals.homework.event;

import com.canals.homework.config.KafkaConfig;
import com.canals.homework.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
  private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

  private final OrderService orderService;

  public OrderEventConsumer(OrderService orderService) {
    this.orderService = orderService;
  }

  @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC, groupId = "order-fulfillment-group")
  public void onOrderCreated(OrderCreatedEvent event) {
    logger.info(
        "Received order creation event for orderId: {} from customer: {}",
        event.getOrderId(),
        event.getCustomer());

    try {
      orderService.fulfillOrder(event.getOrderId());
    } catch (Exception e) {
      logger.error("Error processing order fulfillment for orderId: {}", event.getOrderId(), e);
      throw e;
    }
  }
}
