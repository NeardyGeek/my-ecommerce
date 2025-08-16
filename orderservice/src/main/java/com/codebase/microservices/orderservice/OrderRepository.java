package com.codebase.microservices.orderservice;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends CassandraRepository<Order, UUID> {

    @Query("SELECT * FROM orders WHERE customer_id = ?0 ALLOW FILTERING")
    List<Order> findByCustomerId(String customerId);

    @Query("SELECT * FROM orders WHERE customer_email = ?0 ALLOW FILTERING")
    List<Order> findByCustomerEmail(String customerEmail);

    @Query("SELECT * FROM orders WHERE order_status = ?0 ALLOW FILTERING")
    List<Order> findByOrderStatus(String orderStatus);

    @Query("SELECT * FROM orders WHERE payment_id = ?0 ALLOW FILTERING")
    Order findByPaymentId(String paymentId);
}