package com.codebase.microservices.orderservice;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private Order mockOrder;
    private OrderItem mockOrderItem;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        // Setup mock order item
        mockOrderItem = OrderItem.builder()
                .itemId("item-123")
                .itemName("Test Product")
                .upc("123456789")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(25.99))
                .totalPrice(BigDecimal.valueOf(51.98))
                .itemPictureUrl("http://example.com/image.jpg")
                .build();

        // Setup mock order
        mockOrder = Order.builder()
                .orderId(orderId)
                .customerId("customer-123")
                .customerEmail("test@example.com")
                .orderStatus(OrderStatus.CREATED.name())
                .totalAmount(BigDecimal.valueOf(51.98))
                .currency("USD")
                .shippingAddress("123 Main St")
                .billingAddress("123 Main St")
                .paymentMethod("CREDIT_CARD")
                .orderItems(Arrays.asList(mockOrderItem))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .notes("Test order")
                .build();

        // Setup create order request
        OrderItemDto orderItemDto = OrderItemDto.builder()
                .itemId("item-123")
                .itemName("Test Product")
                .upc("123456789")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(25.99))
                .itemPictureUrl("http://example.com/image.jpg")
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .customerId("customer-123")
                .customerEmail("test@example.com")
                .orderItems(Arrays.asList(orderItemDto))
                .shippingAddress("123 Main St")
                .billingAddress("123 Main St")
                .paymentMethod("CREDIT_CARD")
                .notes("Test order")
                .build();
    }

    @Test
    void createOrder_Success() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        doNothing().when(kafkaProducerService).publishOrderCreated(any(OrderEvent.class));

        // When
        OrderResponse result = orderService.createOrder(createOrderRequest);

        // Then
        assertNotNull(result);
        assertEquals("customer-123", result.getCustomerId());
        assertEquals("test@example.com", result.getCustomerEmail());
        assertEquals(OrderStatus.CREATED.name(), result.getOrderStatus());
        assertEquals(BigDecimal.valueOf(51.98), result.getTotalAmount());
        assertEquals("USD", result.getCurrency());
        assertEquals(1, result.getOrderItems().size());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaProducerService, times(1)).publishOrderCreated(any(OrderEvent.class));
    }

    @Test
    void createOrder_EmptyOrderItems_ThrowsException() {
        // Given
        createOrderRequest.setOrderItems(List.of());

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.createOrder(createOrderRequest));
        verify(orderRepository, never()).save(any(Order.class));
        verify(kafkaProducerService, never()).publishOrderCreated(any(OrderEvent.class));
    }

    @Test
    void getOrder_Success() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // When
        OrderResponse result = orderService.getOrder(orderId);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("customer-123", result.getCustomerId());
        assertEquals(OrderStatus.CREATED.name(), result.getOrderStatus());

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getOrder_NotFound_ThrowsException() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.getOrder(orderId));
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void updateOrder_Success() {
        // Given
        UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
                .shippingAddress("456 New St")
                .notes("Updated notes")
                .build();

        Order updatedOrder = mockOrder.toBuilder()
                .shippingAddress("456 New St")
                .notes("Updated notes")
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        doNothing().when(kafkaProducerService).publishOrderUpdated(any(OrderEvent.class));

        // When
        OrderResponse result = orderService.updateOrder(orderId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("456 New St", result.getShippingAddress());
        assertEquals("Updated notes", result.getNotes());

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaProducerService, times(1)).publishOrderUpdated(any(OrderEvent.class));
    }

    @Test
    void updateOrder_CompletedOrder_ThrowsException() {
        // Given
        mockOrder.setOrderStatus(OrderStatus.COMPLETED.name());
        UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
                .shippingAddress("456 New St")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.updateOrder(orderId, updateRequest));
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_Success() {
        // Given
        Order cancelledOrder = mockOrder.toBuilder()
                .orderStatus(OrderStatus.CANCELLED.name())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);
        doNothing().when(kafkaProducerService).publishOrderCancelled(any(OrderEvent.class));

        // When
        OrderResponse result = orderService.cancelOrder(orderId);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED.name(), result.getOrderStatus());

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaProducerService, times(1)).publishOrderCancelled(any(OrderEvent.class));
    }

    @Test
    void cancelOrder_AlreadyCancelled_ThrowsException() {
        // Given
        mockOrder.setOrderStatus(OrderStatus.CANCELLED.name());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(orderId));
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void completeOrder_Success() {
        // Given
        mockOrder.setOrderStatus(OrderStatus.PAID.name());
        Order completedOrder = mockOrder.toBuilder()
                .orderStatus(OrderStatus.COMPLETED.name())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(completedOrder);
        doNothing().when(kafkaProducerService).publishOrderCompleted(any(OrderEvent.class));

        // When
        OrderResponse result = orderService.completeOrder(orderId);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.COMPLETED.name(), result.getOrderStatus());

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaProducerService, times(1)).publishOrderCompleted(any(OrderEvent.class));
    }

    @Test
    void completeOrder_NotPaid_ThrowsException() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.completeOrder(orderId));
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersByCustomer_Success() {
        // Given
        String customerId = "customer-123";
        List<Order> orders = Collections.singletonList(mockOrder);
        when(orderRepository.findByCustomerId(customerId)).thenReturn(orders);

        // When
        List<OrderResponse> result = orderService.getOrdersByCustomer(customerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(customerId, result.get(0).getCustomerId());

        verify(orderRepository, times(1)).findByCustomerId(customerId);
    }
}