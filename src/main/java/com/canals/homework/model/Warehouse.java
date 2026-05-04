package com.canals.homework.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Entity
@Getter
public class Warehouse {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  List<String>
      products; // should I load all warehouses to memory so I can filter? seems like it would take
  // way too long
  // even cached, so I have to think of another way
  String address;
}
