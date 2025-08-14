package com.codebase.microservices.itemservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ITEM_EVENTS_TOPIC = "item-events";
    private static final String INVENTORY_EVENTS_TOPIC = "inventory-events";

    public void publishItemCreatedEvent(String itemId, String name, String category) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ITEM_CREATED");
        event.put("itemId", itemId);
        event.put("name", name);
        event.put("category", category);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(ITEM_EVENTS_TOPIC, itemId, event);
        log.info("Published ITEM_CREATED event for item: {}", itemId);
    }

    public void publishItemUpdatedEvent(String itemId, String name) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ITEM_UPDATED");
        event.put("itemId", itemId);
        event.put("name", name);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(ITEM_EVENTS_TOPIC, itemId, event);
        log.info("Published ITEM_UPDATED event for item: {}", itemId);
    }

    public void publishItemDeletedEvent(String itemId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ITEM_DELETED");
        event.put("itemId", itemId);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(ITEM_EVENTS_TOPIC, itemId, event);
        log.info("Published ITEM_DELETED event for item: {}", itemId);
    }

    public void publishInventoryUpdatedEvent(String itemId, Integer availableStock, String status) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "INVENTORY_UPDATED");
        event.put("itemId", itemId);
        event.put("availableStock", availableStock);
        event.put("status", status);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, itemId, event);
        log.info("Published INVENTORY_UPDATED event for item: {} with stock: {}", itemId, availableStock);
    }

    public void publishLowStockAlertEvent(String itemId, Integer currentStock, Integer minStockLevel) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "LOW_STOCK_ALERT");
        event.put("itemId", itemId);
        event.put("currentStock", currentStock);
        event.put("minStockLevel", minStockLevel);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, itemId, event);
        log.info("Published LOW_STOCK_ALERT event for item: {} with stock: {}", itemId, currentStock);
    }

    public void publishInventoryReservedEvent(String itemId, Integer reservedQuantity, String orderId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "INVENTORY_RESERVED");
        event.put("itemId", itemId);
        event.put("reservedQuantity", reservedQuantity);
        event.put("orderId", orderId);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, itemId, event);
        log.info("Published INVENTORY_RESERVED event for item: {} with quantity: {}", itemId, reservedQuantity);
    }

    public void publishInventoryReleasedEvent(String itemId, Integer releasedQuantity, String orderId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "INVENTORY_RELEASED");
        event.put("itemId", itemId);
        event.put("releasedQuantity", releasedQuantity);
        event.put("orderId", orderId);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, itemId, event);
        log.info("Published INVENTORY_RELEASED event for item: {} with quantity: {}", itemId, releasedQuantity);
    }
}