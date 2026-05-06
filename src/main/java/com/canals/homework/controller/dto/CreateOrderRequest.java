package com.canals.homework.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
  @NotBlank(message = "Customer name is required")
  private String customer;

  @NotBlank(message = "Shipping address is required")
  private String shippingAddress;

  @NotBlank(message = "Credit card number is required")
  private String creditCardNumber;

  @NotEmpty(message = "At least one item is required")
  private List<@Valid OrderItemRequest> items;
}
