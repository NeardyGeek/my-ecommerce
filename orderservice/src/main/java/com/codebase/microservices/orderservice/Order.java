package com.codebase.microservices.orderservice;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @PrimaryKey
    @Column("order_id")
    private UUID orderId;

    @Column("customer_id")
    private String customerId;

    @Column("customer_email")
    private String customerEmail;

    @Column("order_status")
    private String orderStatus; // CREATED, PAYMENT_PENDING, PAID, COMPLETED, CANCELLED

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("currency")
    private String currency;

    @Column("shipping_address")
    private String shippingAddress;

    @Column("billing_address")
    private String billingAddress;

    @Column("payment_method")
    private String paymentMethod;

    @Column("order_items")
    private List<OrderItem> orderItems;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("payment_id")
    private String paymentId;

    @Column("notes")
    private String notes;
}