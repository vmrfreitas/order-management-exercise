package com.canals.homework.event;

import com.canals.homework.config.KafkaConfig;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
  private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publishOrderCreated(UUID orderId, String customer, String shippingAddress) {
    try {
      var createdEvent = new OrderCreatedEvent(orderId, customer, shippingAddress);

      kafkaTemplate
          .send(KafkaConfig.ORDER_CREATED_TOPIC, orderId.toString(), createdEvent)
          .whenComplete(
              (result, ex) -> {
                if (ex == null) {
                  logger.info("Order creation event published for orderId: {}", orderId);
                } else {
                  logger.error(
                      "Failed to publish order creation event for orderId: {}", orderId, ex);
                }
              });
    } catch (Exception e) {
      logger.error("Exception while publishing order creation event for orderId: {}", orderId, e);
    }
  }
}
