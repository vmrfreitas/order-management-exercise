package com.canals.homework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.canals.homework.controller.dto.CreateOrderResponse;
import com.canals.homework.model.OrderStatus;
import com.canals.homework.repository.OrderRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = {"order-created"})
@TestPropertySource(
    properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class OrderE2EIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private RestClient.Builder restClientBuilder;

  @Autowired private OrderRepository orderRepository;

  @Test
  void testCreateAndFulfillOrder() throws Exception {
    // 1. Prepare request data matching the seeded products in data.sql
    var request =
        Map.of(
            "customer", "Integration Test User",
            "shippingAddress", "123 Storage Ave, New York, NY 10001",
            "creditCardNumber", "4111111111111111",
            "items",
                List.of(
                    Map.of(
                        "productId",
                        "550e8400-e29b-41d4-a716-446655440001", // Laptop
                        "quantity",
                        1),
                    Map.of(
                        "productId",
                        "550e8400-e29b-41d4-a716-446655440002", // Mouse
                        "quantity",
                        2)));

    var restClient = restClientBuilder.baseUrl("http://localhost:" + port).build();

    // 2. Perform POST /orders and extract the orderId
    var response =
        restClient
            .post()
            .uri("/orders")
            .body(request)
            .retrieve()
            .toEntity(CreateOrderResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getOrderId()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo("PENDING");

    var orderId = response.getBody().getOrderId();

    // 3. Verify order is initially in PENDING status in the DB
    var order = orderRepository.findById(orderId).orElseThrow();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

    // 4. Wait for async fulfillment via Kafka
    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () -> {
              var updatedOrder = orderRepository.findById(orderId).orElseThrow();
              assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.FULFILLED);
            });

    // 5. Verify the GET /orders/{id} endpoint returns the fulfilled order
    var getResponse =
        restClient
            .get()
            .uri("/orders/" + orderId)
            .retrieve()
            .toEntity(
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    assertThat(getResponse.getBody().get("status")).isEqualTo("FULFILLED");
    assertThat(getResponse.getBody().get("customer")).isEqualTo("Integration Test User");

    var items = (List<?>) getResponse.getBody().get("items");
    assertThat(items).hasSize(2);
  }
}
