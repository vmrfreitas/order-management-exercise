package com.canals.homework;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(partitions = 1)
@TestPropertySource(
    properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class HomeworkApplicationTests {

  @Test
  void contextLoads() {}
}
