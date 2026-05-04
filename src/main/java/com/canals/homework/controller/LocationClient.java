package com.canals.homework.controller;

import org.springframework.stereotype.Component;

@Component
public class LocationClient {
  public Double getDistance(String address1, String address2) {
    // Mock implementation for distance calculation
    // In a real application, this would call an external API like Google Maps
    return 10.0; // Return a fixed distance for demonstration purposes
  }
}
