package com.codebase.microservices.itemservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ItemController {

    private final ItemService itemService;

    // ============= ITEM MANAGEMENT ENDPOINTS =============

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        log.info("Creating new item: {}", request.getName());
        ItemResponse response = itemService.createItem(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable String itemId) {
        log.info("Fetching item by ID: {}", itemId);
        ItemResponse response = itemService.getItemById(itemId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upc/{upc}")
    public ResponseEntity<ItemResponse> getItemByUpc(@PathVariable String upc) {
        log.info("Fetching item by UPC: {}", upc);
        ItemResponse response = itemService.getItemByUpc(upc);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        log.info("Fetching items - category: {}, search: {}, activeOnly: {}", category, search, activeOnly);

        List<ItemResponse> items;
        if (search != null && !search.trim().isEmpty()) {
            items = itemService.searchItems(search);
        } else if (category != null && !category.trim().isEmpty()) {
            items = itemService.getItemsByCategory(category);
        } else if (activeOnly) {
            items = itemService.getActiveItems();
        } else {
            items = itemService.getAllItems();
        }

        return ResponseEntity.ok(items);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        log.info("Updating item: {}", itemId);
        ItemResponse response = itemService.updateItem(itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable String itemId) {
        log.info("Deleting item: {}", itemId);
        itemService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }

    // ============= INVENTORY MANAGEMENT ENDPOINTS =============

    @GetMapping("/{itemId}/inventory")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String itemId) {
        log.info("Fetching inventory for item: {}", itemId);
        InventoryResponse response = itemService.getInventory(itemId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{itemId}/availability")
    public ResponseEntity<InventoryCheckResponse> checkAvailability(
            @PathVariable String itemId,
            @RequestParam Integer quantity) {
        log.info("Checking availability for item: {} with quantity: {}", itemId, quantity);
        InventoryCheckResponse response = itemService.checkAvailability(itemId, quantity);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}/inventory")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable String itemId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        log.info("Updating inventory for item: {}", itemId);
        InventoryResponse response = itemService.updateInventory(itemId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory/reserve")
    public ResponseEntity<Boolean> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        log.info("Reserving inventory for item: {} with quantity: {}",
                request.getItemId(), request.getQuantity());
        boolean reserved = itemService.reserveInventory(request);
        return ResponseEntity.ok(reserved);
    }

    @PostMapping("/{itemId}/inventory/release")
    public ResponseEntity<Void> releaseReservedInventory(
            @PathVariable String itemId,
            @RequestParam Integer quantity) {
        log.info("Releasing reserved inventory for item: {} with quantity: {}", itemId, quantity);
        itemService.releaseReservedInventory(itemId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{itemId}/details")
    public ResponseEntity<ItemWithInventoryResponse> getItemWithInventory(@PathVariable String itemId) {
        log.info("Fetching item with inventory details: {}", itemId);
        ItemWithInventoryResponse response = itemService.getItemWithInventory(itemId);
        return ResponseEntity.ok(response);
    }

    // ============= INVENTORY MONITORING ENDPOINTS =============

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        log.info("Fetching low stock items");
        List<InventoryResponse> response = itemService.getLowStockItems();
        return ResponseEntity.ok(response);
    }

    // ============= HEALTH CHECK ENDPOINT =============

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Item Service is running");
    }
}