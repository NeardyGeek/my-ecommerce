package com.codebase.microservices.orderservice;

public enum OrderStatus {
    CREATED("Order has been created"),
    PAYMENT_PENDING("Payment is being processed"),
    PAID("Payment has been completed"),
    COMPLETED("Order has been completed"),
    CANCELLED("Order has been cancelled"),
    FAILED("Order processing failed");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}