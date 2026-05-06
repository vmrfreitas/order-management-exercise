package com.canals.homework.controller;

import org.springframework.stereotype.Component;

@Component
public class LocationClient {
  public Double getDistance(String address1, String address2) {
    // Mock implementation for distance calculation
    // In a real application, this would call an external API like Google Maps
    if (address1 == null || address2 == null) {
      return 0.0;
    }

    // Generate a deterministic mock distance based on address hash codes
    // This ensures the same addresses always return the same distance
    var addressHashCombined = (address1.hashCode() ^ address2.hashCode()) & 0x7FFFFFFF;
    // Return a distance between 5 and 100 km
    return 5.0 + (addressHashCombined % 96);
  }
}
