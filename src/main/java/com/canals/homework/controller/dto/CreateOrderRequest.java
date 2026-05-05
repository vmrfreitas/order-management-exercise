package com.canals.homework.controller.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
  private String customer;
  private String shippingAddress;
  private List<OrderItemRequest> items;
}
