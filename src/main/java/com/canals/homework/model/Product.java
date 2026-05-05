package com.canals.homework.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID productId;

  private String name;
}
