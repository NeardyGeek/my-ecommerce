package com.codebase.microservices.itemservice;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {

    Optional<Item> findByItemId(String itemId);

    Optional<Item> findByUpc(String upc);

    List<Item> findByCategory(String category);

    List<Item> findByBrand(String brand);

    List<Item> findByStatus(String status);

    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<Item> findByNameContainingIgnoreCase(String name);

    @Query("{'$or': [" +
            "{'name': {$regex: ?0, $options: 'i'}}, " +
            "{'description': {$regex: ?0, $options: 'i'}}, " +
            "{'tags': {$in: [?0]}}, " +
            "{'category': {$regex: ?0, $options: 'i'}}" +
            "]}")
    List<Item> searchItems(String searchTerm);

    List<Item> findByPriceBetween(Double minPrice, Double maxPrice);

    @Query("{'status': 'ACTIVE'}")
    List<Item> findActiveItems();
}