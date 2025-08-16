package com.codebase.microservices.orderservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    public void publishOrderEvent(OrderEvent orderEvent) {
        try {
            log.info("Publishing order event: {} for order: {}",
                    orderEvent.getEventType(), orderEvent.getOrderId());

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(orderEventsTopic, orderEvent.getOrderId().toString(), orderEvent);

            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Order event published successfully: {} with offset: {}",
                            orderEvent.getEventType(), result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish order event: {}", orderEvent.getEventType(), exception);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing order event: {}", orderEvent.getEventType(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }

    public void publishOrderCreated(OrderEvent orderEvent) {
        orderEvent.setEventType("ORDER_CREATED");
        publishOrderEvent(orderEvent);
    }

    public void publishOrderUpdated(OrderEvent orderEvent) {
        orderEvent.setEventType("ORDER_UPDATED");
        publishOrderEvent(orderEvent);
    }

    public void publishOrderCancelled(OrderEvent orderEvent) {
        orderEvent.setEventType("ORDER_CANCELLED");
        publishOrderEvent(orderEvent);
    }

    public void publishOrderCompleted(OrderEvent orderEvent) {
        orderEvent.setEventType("ORDER_COMPLETED");
        publishOrderEvent(orderEvent);
    }
}