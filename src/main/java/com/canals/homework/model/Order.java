package com.canals.homework.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String customer;

    private String shippingAddress;

    private List<String> items;
}
