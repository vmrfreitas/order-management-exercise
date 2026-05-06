package com.canals.homework.controller.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateOrderResponse {
  private UUID orderId;
  private String status;
  private String message;
}
