package com.codebase.microservices.itemservice;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends MongoRepository<Inventory, String> {

    Optional<Inventory> findByItemId(String itemId);

    List<Inventory> findByStatus(String status);

    @Query("{'availableStock': {$lte: ?0}}")
    List<Inventory> findLowStockItems(Integer threshold);

    @Query("{'availableStock': {$lte: '$minStockLevel'}}")
    List<Inventory> findItemsBelowMinStock();

    @Query("{'availableStock': 0}")
    List<Inventory> findOutOfStockItems();

    List<Inventory> findByWarehouseLocation(String location);
}