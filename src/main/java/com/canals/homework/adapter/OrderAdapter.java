package com.canals.homework.adapter;

import com.canals.homework.controller.dto.CreateOrderRequest;
import com.canals.homework.model.Order;
import com.canals.homework.model.OrderItem;
import com.canals.homework.repository.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderAdapter {
  private final ProductRepository productRepository;

  public OrderAdapter(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public Order toDomain(CreateOrderRequest request) {
    var newOrder = new Order();
    newOrder.setCustomer(request.getCustomer());
    newOrder.setShippingAddress(request.getShippingAddress());
    newOrder.setCreditCardNumber(request.getCreditCardNumber());

    var orderItems =
        request.getItems().stream()
            .map(
                itemRequest -> {
                  var foundProduct =
                      productRepository
                          .findById(itemRequest.getProductId())
                          .orElseThrow(
                              () ->
                                  new IllegalArgumentException(
                                      "Product not found: " + itemRequest.getProductId()));

                  var newOrderItem = new OrderItem();
                  newOrderItem.setOrder(newOrder);
                  newOrderItem.setProduct(foundProduct);
                  newOrderItem.setQuantity(itemRequest.getQuantity());
                  return newOrderItem;
                })
            .toList();

    newOrder.setItems(orderItems);
    return newOrder;
  }
}
