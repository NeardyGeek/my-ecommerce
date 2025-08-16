package com.codebase.microservices.orderservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "APIs for managing orders")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order for the authenticated customer")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<Map<String, Object>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Just validate that user is authenticated (like Item Service)
            // Use the customer info from the request body directly

            OrderResponse orderResponse = orderService.createOrder(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully");
            response.put("order", orderResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating order", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its ID")
    @ApiResponse(responseCode = "200", description = "Order retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<Map<String, Object>> getOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {

        try {
            OrderResponse orderResponse = orderService.getOrder(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", orderResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving order: {}", orderId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Order not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/customer")
    @Operation(summary = "Get customer orders", description = "Retrieves all orders for the authenticated customer")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    public ResponseEntity<Map<String, Object>> getCustomerOrders(HttpServletRequest httpRequest) {

        try {
            // Try to get customerId from JWT token first
            String customerId = (String) httpRequest.getAttribute("customerId");

            // If no customerId from token, fall back to query parameter
            if (customerId == null) {
                customerId = httpRequest.getParameter("customerId");
            }

            if (customerId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Customer ID is required (either in JWT token or as query parameter)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("totalOrders", orders.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving customer orders", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve orders");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{orderId}")
    @Operation(summary = "Update order", description = "Updates an existing order")
    @ApiResponse(responseCode = "200", description = "Order updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or order cannot be updated")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<Map<String, Object>> updateOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderRequest request) {

        try {
            OrderResponse updatedOrder = orderService.updateOrder(orderId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order updated successfully");
            response.put("order", updatedOrder);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating order: {}", orderId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order", description = "Cancels an existing order")
    @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    @ApiResponse(responseCode = "400", description = "Order cannot be cancelled")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {

        try {
            OrderResponse cancelledOrder = orderService.cancelOrder(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order cancelled successfully");
            response.put("order", cancelledOrder);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cancelling order: {}", orderId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to cancel order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{orderId}/complete")
    @Operation(summary = "Complete order", description = "Marks an order as completed (admin/system use)")
    @ApiResponse(responseCode = "200", description = "Order completed successfully")
    @ApiResponse(responseCode = "400", description = "Order cannot be completed")
    public ResponseEntity<Map<String, Object>> completeOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {

        try {
            OrderResponse completedOrder = orderService.completeOrder(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order completed successfully");
            response.put("order", completedOrder);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error completing order: {}", orderId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to complete order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "order-service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}