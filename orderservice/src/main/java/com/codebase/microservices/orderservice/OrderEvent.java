package com.codebase.microservices.orderservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Order Event DTO for Kafka
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderEvent {

    private String eventType; // ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED, ORDER_COMPLETED
    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private String orderStatus;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemDto> orderItems;
    private String paymentId;
    private LocalDateTime timestamp;
    private String notes;
}
