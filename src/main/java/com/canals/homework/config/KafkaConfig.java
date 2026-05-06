package com.canals.homework.config;

import com.canals.homework.event.OrderCreatedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
public class KafkaConfig {

  public static final String ORDER_CREATED_TOPIC = "order-created";
  public static final String ORDER_FULFILLED_TOPIC = "order-fulfilled";

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public ProducerFactory<String, OrderCreatedEvent> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate(
      ProducerFactory<String, OrderCreatedEvent> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public NewTopic orderCreatedTopic() {
    return TopicBuilder.name(ORDER_CREATED_TOPIC).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic orderFulfilledTopic() {
    return TopicBuilder.name(ORDER_FULFILLED_TOPIC).partitions(1).replicas(1).build();
  }
}
