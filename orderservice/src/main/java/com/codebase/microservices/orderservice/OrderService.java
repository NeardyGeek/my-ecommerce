package com.codebase.microservices.orderservice;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;

    public OrderResponse createOrder(CreateOrderRequest request) {
        try {
            log.info("Creating order for customer: {}", request.getCustomerId());

            if(request.getOrderItems().isEmpty()){
                throw new RuntimeException("orderItems at least contains one item");
            }


            // Calculate total amount
            BigDecimal totalAmount = request.getOrderItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Convert DTOs to entities
            List<OrderItem> orderItems = request.getOrderItems().stream()
                    .map(this::convertToOrderItem)
                    .collect(Collectors.toList());

            // Create order entity
            Order order = Order.builder()
                    .orderId(UUID.randomUUID())
                    .customerId(request.getCustomerId())
                    .customerEmail(request.getCustomerEmail())
                    .orderStatus(OrderStatus.CREATED.name())
                    .totalAmount(totalAmount)
                    .currency("USD")
                    .shippingAddress(request.getShippingAddress())
                    .billingAddress(request.getBillingAddress())
                    .paymentMethod(request.getPaymentMethod())
                    .orderItems(orderItems)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .notes(request.getNotes())
                    .build();

            // Save order
            Order savedOrder = orderRepository.save(order);

            // Publish order created event
            OrderEvent orderEvent = convertToOrderEvent(savedOrder, "ORDER_CREATED");
            kafkaProducerService.publishOrderCreated(orderEvent);

            log.info("Order created successfully with ID: {}", savedOrder.getOrderId());
            return convertToOrderResponse(savedOrder);

        } catch (Exception e) {
            log.error("Error creating order for customer: {}", request.getCustomerId(), e);
            throw new RuntimeException("Failed to create order", e);
        }
    }

    public OrderResponse updateOrder(UUID orderId, UpdateOrderRequest request) {
        try {
            log.info("Updating order: {}", orderId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            // Only allow updates for orders in CREATED or PAYMENT_PENDING status
            if (!order.getOrderStatus().equals(OrderStatus.CREATED.name()) &&
                    !order.getOrderStatus().equals(OrderStatus.PAYMENT_PENDING.name())) {
                throw new RuntimeException("Order cannot be updated in current status: " + order.getOrderStatus());
            }

            // Update order fields
            if (request.getShippingAddress() != null) {
                order.setShippingAddress(request.getShippingAddress());
            }
            if (request.getBillingAddress() != null) {
                order.setBillingAddress(request.getBillingAddress());
            }
            if (request.getPaymentMethod() != null) {
                order.setPaymentMethod(request.getPaymentMethod());
            }
            if (request.getNotes() != null) {
                order.setNotes(request.getNotes());
            }
            if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
                List<OrderItem> updatedItems = request.getOrderItems().stream()
                        .map(this::convertToOrderItem)
                        .collect(Collectors.toList());
                order.setOrderItems(updatedItems);

                // Recalculate total amount
                BigDecimal newTotal = request.getOrderItems().stream()
                        .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                order.setTotalAmount(newTotal);
            }

            order.setUpdatedAt(LocalDateTime.now());

            // Save updated order
            Order updatedOrder = orderRepository.save(order);

            // Publish order updated event
            OrderEvent orderEvent = convertToOrderEvent(updatedOrder, "ORDER_UPDATED");
            kafkaProducerService.publishOrderUpdated(orderEvent);

            log.info("Order updated successfully: {}", orderId);
            return convertToOrderResponse(updatedOrder);

        } catch (Exception e) {
            log.error("Error updating order: {}", orderId, e);
            throw new RuntimeException("Failed to update order", e);
        }
    }

    public OrderResponse cancelOrder(UUID orderId) {
        try {
            log.info("Cancelling order: {}", orderId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            // Only allow cancellation for orders not yet completed
            if (order.getOrderStatus().equals(OrderStatus.COMPLETED.name()) ||
                    order.getOrderStatus().equals(OrderStatus.CANCELLED.name())) {
                throw new RuntimeException("Order cannot be cancelled in current status: " + order.getOrderStatus());
            }

            order.setOrderStatus(OrderStatus.CANCELLED.name());
            order.setUpdatedAt(LocalDateTime.now());
            order.setNotes(order.getNotes() + " | Order cancelled by user");

            // Save cancelled order
            Order cancelledOrder = orderRepository.save(order);

            // Publish order cancelled event
            OrderEvent orderEvent = convertToOrderEvent(cancelledOrder, "ORDER_CANCELLED");
            kafkaProducerService.publishOrderCancelled(orderEvent);

            log.info("Order cancelled successfully: {}", orderId);
            return convertToOrderResponse(cancelledOrder);

        } catch (Exception e) {
            log.error("Error cancelling order: {}", orderId, e);
            throw new RuntimeException("Failed to cancel order", e);
        }
    }

    public OrderResponse getOrder(UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            return convertToOrderResponse(order);

        } catch (Exception e) {
            log.error("Error retrieving order: {}", orderId, e);
            throw new RuntimeException("Failed to retrieve order", e);
        }
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        try {
            List<Order> orders = orderRepository.findByCustomerId(customerId);
            return orders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error retrieving orders for customer: {}", customerId, e);
            throw new RuntimeException("Failed to retrieve orders", e);
        }
    }

    public OrderResponse completeOrder(UUID orderId) {
        try {
            log.info("Completing order: {}", orderId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            // Only allow completion for paid orders
            if (!order.getOrderStatus().equals(OrderStatus.PAID.name())) {
                throw new RuntimeException("Order must be paid before completion. Current status: " + order.getOrderStatus());
            }

            order.setOrderStatus(OrderStatus.COMPLETED.name());
            order.setUpdatedAt(LocalDateTime.now());

            // Save completed order
            Order completedOrder = orderRepository.save(order);

            // Publish order completed event
            OrderEvent orderEvent = convertToOrderEvent(completedOrder, "ORDER_COMPLETED");
            kafkaProducerService.publishOrderCompleted(orderEvent);

            log.info("Order completed successfully: {}", orderId);
            return convertToOrderResponse(completedOrder);

        } catch (Exception e) {
            log.error("Error completing order: {}", orderId, e);
            throw new RuntimeException("Failed to complete order", e);
        }
    }

    // Conversion methods
    private OrderItem convertToOrderItem(OrderItemDto dto) {
        BigDecimal totalPrice = dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));

        return OrderItem.builder()
                .itemId(dto.getItemId())
                .itemName(dto.getItemName())
                .upc(dto.getUpc())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .totalPrice(totalPrice)
                .itemPictureUrl(dto.getItemPictureUrl())
                .build();
    }

    private OrderItemDto convertToOrderItemDto(OrderItem orderItem) {
        return OrderItemDto.builder()
                .itemId(orderItem.getItemId())
                .itemName(orderItem.getItemName())
                .upc(orderItem.getUpc())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .itemPictureUrl(orderItem.getItemPictureUrl())
                .build();
    }

    private OrderResponse convertToOrderResponse(Order order) {
        List<OrderItemDto> orderItemDtos = order.getOrderItems().stream()
                .map(this::convertToOrderItemDto)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .paymentMethod(order.getPaymentMethod())
                .orderItems(orderItemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paymentId(order.getPaymentId())
                .notes(order.getNotes())
                .build();
    }

    public OrderEvent convertToOrderEvent(Order order, String eventType) {
        List<OrderItemDto> orderItemDtos = order.getOrderItems().stream()
                .map(this::convertToOrderItemDto)
                .collect(Collectors.toList());

        return OrderEvent.builder()
                .eventType(eventType)
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .orderItems(orderItemDtos)
                .paymentId(order.getPaymentId())
                .timestamp(LocalDateTime.now())
                .notes(order.getNotes())
                .build();
    }
}