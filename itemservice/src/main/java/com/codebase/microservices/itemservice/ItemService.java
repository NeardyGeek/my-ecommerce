package com.codebase.microservices.itemservice;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ItemEventProducer eventProducer;



    // Update the createItem method to include event publishing
    @Transactional
    public ItemResponse createItem(CreateItemRequest request) {
        log.info("Creating new item with name: {}", request.getName());

        // Check if UPC already exists
        if (itemRepository.findByUpc(request.getUpc()).isPresent()) {
            throw new InvalidOperationException("Item with UPC " + request.getUpc() + " already exists");
        }

        // Create item
        Item item = new Item();
        item.setItemId(generateItemId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        item.setBrand(request.getBrand());
        item.setUpc(request.getUpc());
        item.setPrice(request.getPrice());
        item.setCurrency(request.getCurrency());
        item.setImageUrls(request.getImageUrls());
        item.setSpecifications(request.getSpecifications());
        item.setStatus("ACTIVE");
        item.setTags(request.getTags());
        item.setSku(request.getSku());
        item.setWeight(request.getWeight());
        item.setDimensions(request.getDimensions());
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        Item savedItem = itemRepository.save(item);

        // Create initial inventory
        Inventory inventory = new Inventory();
        inventory.setItemId(savedItem.getItemId());
        inventory.setTotalStock(request.getInitialStock());
        inventory.setAvailableStock(request.getInitialStock());
        inventory.setReservedStock(0);
        inventory.setMinStockLevel(request.getMinStockLevel());
        inventory.setMaxStockLevel(request.getMaxStockLevel());
        inventory.setWarehouseLocation(request.getWarehouseLocation());
        inventory.setLastRestocked(LocalDateTime.now());
        inventory.setLastUpdated(LocalDateTime.now());
        inventory.setStatus(determineInventoryStatus(request.getInitialStock(), request.getMinStockLevel()));

        inventoryRepository.save(inventory);

        // Publish event
        eventProducer.publishItemCreatedEvent(savedItem.getItemId(), savedItem.getName(), savedItem.getCategory());

        log.info("Successfully created item with ID: {}", savedItem.getItemId());
        return mapToItemResponse(savedItem);
    }

    // Update the updateItem method to include event publishing
    @Transactional
    public ItemResponse updateItem(String itemId, UpdateItemRequest request) {
        log.info("Updating item with ID: {}", itemId);

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with ID: " + itemId));

        // Update fields if provided
        if (request.getName() != null) item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getCategory() != null) item.setCategory(request.getCategory());
        if (request.getBrand() != null) item.setBrand(request.getBrand());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCurrency() != null) item.setCurrency(request.getCurrency());
        if (request.getImageUrls() != null) item.setImageUrls(request.getImageUrls());
        if (request.getSpecifications() != null) item.setSpecifications(request.getSpecifications());
        if (request.getStatus() != null) item.setStatus(request.getStatus());
        if (request.getTags() != null) item.setTags(request.getTags());
        if (request.getSku() != null) item.setSku(request.getSku());
        if (request.getWeight() != null) item.setWeight(request.getWeight());
        if (request.getDimensions() != null) item.setDimensions(request.getDimensions());

        item.setUpdatedAt(LocalDateTime.now());

        Item updatedItem = itemRepository.save(item);

        // Publish event
        eventProducer.publishItemUpdatedEvent(updatedItem.getItemId(), updatedItem.getName());

        log.info("Successfully updated item with ID: {}", itemId);
        return mapToItemResponse(updatedItem);
    }

    // Update the deleteItem method to include event publishing
    @Transactional
    public void deleteItem(String itemId) {
        log.info("Deleting item with ID: {}", itemId);

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with ID: " + itemId));

        // Soft delete - mark as discontinued
        item.setStatus("DISCONTINUED");
        item.setUpdatedAt(LocalDateTime.now());
        itemRepository.save(item);

        // Update inventory status
        inventoryRepository.findByItemId(itemId).ifPresent(inventory -> {
            inventory.setStatus("DISCONTINUED");
            inventory.setLastUpdated(LocalDateTime.now());
            inventoryRepository.save(inventory);
        });

        // Publish event
        eventProducer.publishItemDeletedEvent(itemId);

        log.info("Successfully deleted item with ID: {}", itemId);
    }

    // Update the updateInventory method to include event publishing
    @Transactional
    public InventoryResponse updateInventory(String itemId, InventoryUpdateRequest request) {
        log.info("Updating inventory for item ID: {} with operation: {}", itemId, request.getOperation());

        Inventory inventory = inventoryRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Inventory not found for item ID: " + itemId));

        int newStock;
        switch (request.getOperation().toUpperCase()) {
            case "ADD":
                newStock = inventory.getTotalStock() + request.getQuantity();
                inventory.setAvailableStock(inventory.getAvailableStock() + request.getQuantity());
                break;
            case "SUBTRACT":
                if (inventory.getAvailableStock() < request.getQuantity()) {
                    throw new InsufficientStockException("Cannot subtract more than available stock");
                }
                newStock = inventory.getTotalStock() - request.getQuantity();
                inventory.setAvailableStock(inventory.getAvailableStock() - request.getQuantity());
                break;
            case "SET":
                newStock = request.getQuantity();
                inventory.setAvailableStock(request.getQuantity() - inventory.getReservedStock());
                break;
            default:
                throw new InvalidOperationException("Invalid operation: " + request.getOperation());
        }

        inventory.setTotalStock(newStock);
        String oldStatus = inventory.getStatus();
        inventory.setStatus(determineInventoryStatus(inventory.getAvailableStock(), inventory.getMinStockLevel()));
        inventory.setLastUpdated(LocalDateTime.now());

        if (request.getWarehouseLocation() != null) {
            inventory.setWarehouseLocation(request.getWarehouseLocation());
        }

        Inventory updatedInventory = inventoryRepository.save(inventory);

        // Publish inventory update event
        eventProducer.publishInventoryUpdatedEvent(itemId, updatedInventory.getAvailableStock(), updatedInventory.getStatus());

        // Publish low stock alert if needed
        if (updatedInventory.getAvailableStock() <= updatedInventory.getMinStockLevel() &&
                !oldStatus.equals("LOW_STOCK") && !oldStatus.equals("OUT_OF_STOCK")) {
            eventProducer.publishLowStockAlertEvent(itemId, updatedInventory.getAvailableStock(), updatedInventory.getMinStockLevel());
        }

        log.info("Successfully updated inventory for item ID: {}", itemId);
        return mapToInventoryResponse(updatedInventory);
    }

    // Update the reserveInventory method to include event publishing
    @Transactional
    public boolean reserveInventory(ReserveInventoryRequest request) {
        log.info("Reserving inventory for item ID: {} with quantity: {}", request.getItemId(), request.getQuantity());

        Inventory inventory = inventoryRepository.findByItemId(request.getItemId())
                .orElseThrow(() -> new ItemNotFoundException("Inventory not found for item ID: " + request.getItemId()));

        if (inventory.getAvailableStock() < request.getQuantity()) {
            log.warn("Insufficient stock for reservation. Available: {}, Requested: {}",
                    inventory.getAvailableStock(), request.getQuantity());
            return false;
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - request.getQuantity());
        inventory.setReservedStock(inventory.getReservedStock() + request.getQuantity());
        inventory.setStatus(determineInventoryStatus(inventory.getAvailableStock(), inventory.getMinStockLevel()));
        inventory.setLastUpdated(LocalDateTime.now());

        inventoryRepository.save(inventory);

        // Publish reservation event
        eventProducer.publishInventoryReservedEvent(request.getItemId(), request.getQuantity(), request.getOrderId());

        log.info("Successfully reserved {} units for item ID: {}", request.getQuantity(), request.getItemId());
        return true;
    }

    // Update the releaseReservedInventory method to include event publishing
    @Transactional
    public void releaseReservedInventory(String itemId, Integer quantity) {
        log.info("Releasing reserved inventory for item ID: {} with quantity: {}", itemId, quantity);

        Inventory inventory = inventoryRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Inventory not found for item ID: " + itemId));

        if (inventory.getReservedStock() < quantity) {
            throw new InvalidOperationException("Cannot release more than reserved stock");
        }

        inventory.setAvailableStock(inventory.getAvailableStock() + quantity);
        inventory.setReservedStock(inventory.getReservedStock() - quantity);
        inventory.setStatus(determineInventoryStatus(inventory.getAvailableStock(), inventory.getMinStockLevel()));
        inventory.setLastUpdated(LocalDateTime.now());

        inventoryRepository.save(inventory);

        // Publish release event
        eventProducer.publishInventoryReleasedEvent(itemId, quantity, null);

        log.info("Successfully released {} reserved units for item ID: {}", quantity, itemId);
    }


    public ItemResponse getItemById(String itemId) {
        log.info("Fetching item with ID: {}", itemId);
        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with ID: " + itemId));
        return mapToItemResponse(item);
    }

    public ItemResponse getItemByUpc(String upc) {
        log.info("Fetching item with UPC: {}", upc);
        Item item = itemRepository.findByUpc(upc)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with UPC: " + upc));
        return mapToItemResponse(item);
    }

    public List<ItemResponse> getAllItems() {
        log.info("Fetching all items");
        return itemRepository.findAll().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    public List<ItemResponse> getActiveItems() {
        log.info("Fetching active items");
        return itemRepository.findActiveItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    public List<ItemResponse> getItemsByCategory(String category) {
        log.info("Fetching items by category: {}", category);
        return itemRepository.findByCategory(category).stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    public List<ItemResponse> searchItems(String searchTerm) {
        log.info("Searching items with term: {}", searchTerm);
        return itemRepository.searchItems(searchTerm).stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }





    // ============= INVENTORY METHODS =============

    public InventoryResponse getInventory(String itemId) {
        log.info("Fetching inventory for item ID: {}", itemId);
        Inventory inventory = inventoryRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Inventory not found for item ID: " + itemId));
        return mapToInventoryResponse(inventory);
    }

    public InventoryCheckResponse checkAvailability(String itemId, Integer quantity) {
        log.info("Checking availability for item ID: {} with quantity: {}", itemId, quantity);

        Inventory inventory = inventoryRepository.findByItemId(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Inventory not found for item ID: " + itemId));

        boolean available = inventory.getAvailableStock() >= quantity;
        String message = available ? "Stock available" : "Insufficient stock";

        return new InventoryCheckResponse(itemId, available, inventory.getAvailableStock(), message);
    }




    public ItemWithInventoryResponse getItemWithInventory(String itemId) {
        log.info("Fetching item with inventory for ID: {}", itemId);

        ItemResponse item = getItemById(itemId);
        InventoryResponse inventory = getInventory(itemId);

        return new ItemWithInventoryResponse(item, inventory);
    }

    public List<InventoryResponse> getLowStockItems() {
        log.info("Fetching low stock items");
        return inventoryRepository.findItemsBelowMinStock().stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    // ============= HELPER METHODS =============

    private String generateItemId() {
        return "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String determineInventoryStatus(Integer availableStock, Integer minStockLevel) {
        if (availableStock == 0) {
            return "OUT_OF_STOCK";
        } else if (availableStock <= minStockLevel) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }

    private ItemResponse mapToItemResponse(Item item) {
        ItemResponse response = new ItemResponse();
        response.setId(item.getId());
        response.setItemId(item.getItemId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setCategory(item.getCategory());
        response.setBrand(item.getBrand());
        response.setUpc(item.getUpc());
        response.setPrice(item.getPrice());
        response.setCurrency(item.getCurrency());
        response.setImageUrls(item.getImageUrls());
        response.setSpecifications(item.getSpecifications());
        response.setStatus(item.getStatus());
        response.setTags(item.getTags());
        response.setSku(item.getSku());
        response.setWeight(item.getWeight());
        response.setDimensions(item.getDimensions());
        response.setCreatedAt(item.getCreatedAt() != null ? item.getCreatedAt().format(formatter) : null);
        response.setUpdatedAt(item.getUpdatedAt() != null ? item.getUpdatedAt().format(formatter) : null);
        return response;
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        InventoryResponse response = new InventoryResponse();
        response.setItemId(inventory.getItemId());
        response.setTotalStock(inventory.getTotalStock());
        response.setAvailableStock(inventory.getAvailableStock());
        response.setReservedStock(inventory.getReservedStock());
        response.setStatus(inventory.getStatus());
        response.setWarehouseLocation(inventory.getWarehouseLocation());
        response.setLastUpdated(inventory.getLastUpdated() != null ?
                inventory.getLastUpdated().format(formatter) : null);
        return response;
    }
}