package com.codebase.microservices.orderservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentEvent(PaymentEvent paymentEvent) {
        try {
            log.info("Received payment event: {} for order: {}",
                    paymentEvent.getEventType(), paymentEvent.getOrderId());

            Order order = orderRepository.findById(paymentEvent.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Order not found for payment event: {}", paymentEvent.getOrderId());
                return;
            }

            switch (paymentEvent.getEventType()) {
                case "PAYMENT_SUCCESS":
                    handlePaymentSuccess(order, paymentEvent);
                    break;
                case "PAYMENT_FAILED":
                    handlePaymentFailure(order, paymentEvent);
                    break;
                case "PAYMENT_REFUNDED":
                    handlePaymentRefund(order, paymentEvent);
                    break;
                default:
                    log.info("Unhandled payment event type: {}", paymentEvent.getEventType());
            }

        } catch (Exception e) {
            log.error("Error processing payment event: {}", paymentEvent.getEventType(), e);
        }
    }

    private void handlePaymentSuccess(Order order, PaymentEvent paymentEvent) {
        order.setOrderStatus(OrderStatus.PAID.name());
        order.setPaymentId(paymentEvent.getPaymentId());
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);

        // Publish order paid event
        kafkaProducerService.publishOrderEvent(orderService.convertToOrderEvent(order, "ORDER_PAID"));

        log.info("Order {} marked as PAID", order.getOrderId());
    }

    private void handlePaymentFailure(Order order, PaymentEvent paymentEvent) {
        order.setOrderStatus(OrderStatus.FAILED.name());
        order.setUpdatedAt(LocalDateTime.now());
        order.setNotes(order.getNotes() + " | Payment failed: " + paymentEvent.getFailureReason());

        orderRepository.save(order);

        // Publish order failed event
        kafkaProducerService.publishOrderEvent(orderService.convertToOrderEvent(order, "ORDER_FAILED"));

        log.info("Order {} marked as FAILED due to payment failure", order.getOrderId());
    }

    private void handlePaymentRefund(Order order, PaymentEvent paymentEvent) {
        if (order.getOrderStatus().equals(OrderStatus.PAID.name()) ||
                order.getOrderStatus().equals(OrderStatus.COMPLETED.name())) {

            order.setOrderStatus(OrderStatus.CANCELLED.name());
            order.setUpdatedAt(LocalDateTime.now());
            order.setNotes(order.getNotes() + " | Payment refunded");

            orderRepository.save(order);

            // Publish order cancelled event
            kafkaProducerService.publishOrderCancelled(orderService.convertToOrderEvent(order, "ORDER_CANCELLED"));

            log.info("Order {} cancelled due to payment refund", order.getOrderId());
        }
    }

    // Listen to inventory events (for future integration)
    @KafkaListener(topics = "inventory-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryEvent(String inventoryEventJson) {
        try {
            log.info("Received inventory event: {}", inventoryEventJson);
            // Handle inventory updates, stock availability, etc.
            // This is a placeholder for future inventory service integration
        } catch (Exception e) {
            log.error("Error processing inventory event", e);
        }
    }
}