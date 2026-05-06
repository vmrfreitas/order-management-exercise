package com.canals.homework.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentClient {
  private static final Logger logger = LoggerFactory.getLogger(PaymentClient.class);

  /**
   * Processes a payment through the external payment API.
   *
   * @param creditCardNumber the customer's credit card number
   * @param amount the total amount to charge
   * @param description a description of the payment
   * @return true if the payment was successful, false otherwise
   */
  public boolean processPayment(String creditCardNumber, Double amount, String description) {
    // Mock implementation for payment processing
    // In a real application, this would call an external payment API
    logger.info(
        "Processing payment: card=****{}, amount={}, description={}",
        creditCardNumber != null && creditCardNumber.length() >= 4
            ? creditCardNumber.substring(creditCardNumber.length() - 4)
            : "????",
        amount,
        description);

    // Simulate a successful payment
    logger.info("Payment processed successfully");
    return true;
  }
}
