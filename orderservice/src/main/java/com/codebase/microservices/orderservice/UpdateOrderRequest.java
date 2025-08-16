package com.codebase.microservices.orderservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Update Order Request DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {

    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    private String notes;
    private List<OrderItemDto> orderItems;
}
