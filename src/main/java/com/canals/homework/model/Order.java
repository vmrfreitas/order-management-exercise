package com.canals.homework.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String customer;

  private String shippingAddress;

  @OneToMany(
      mappedBy = "order",
      cascade = jakarta.persistence.CascadeType.ALL,
      orphanRemoval = true)
  private List<OrderItem> items;

  @Enumerated private OrderStatus status = OrderStatus.PENDING;
}
