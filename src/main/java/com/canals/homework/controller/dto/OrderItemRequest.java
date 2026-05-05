package com.canals.homework.controller.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {
  private UUID productId;
  private Integer quantity;
}
