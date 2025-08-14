package com.codebase.microservices.itemservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateItem() throws Exception {
        // Given
        CreateItemRequest request = new CreateItemRequest();
        request.setName("Test Item");
        request.setCategory("Electronics");
        request.setUpc("123456789");
        request.setPrice(new BigDecimal("99.99"));
        request.setInitialStock(10);

        ItemResponse response = new ItemResponse();
        response.setItemId("ITEM-123");
        response.setName("Test Item");

        when(itemService.createItem(any(CreateItemRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void testGetItemById() throws Exception {
        // Given
        ItemResponse response = new ItemResponse();
        response.setItemId("ITEM-123");
        response.setName("Test Item");

        when(itemService.getItemById("ITEM-123")).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/items/ITEM-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("ITEM-123"))
                .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void testGetAllItems() throws Exception {
        // Given
        ItemResponse item1 = new ItemResponse();
        item1.setItemId("ITEM-1");
        item1.setName("Item 1");

        ItemResponse item2 = new ItemResponse();
        item2.setItemId("ITEM-2");
        item2.setName("Item 2");

        List<ItemResponse> items = Arrays.asList(item1, item2);

        when(itemService.getAllItems()).thenReturn(items);

        // When & Then
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Item 1"));
    }

    @Test
    void testUpdateItem() throws Exception {
        // Given
        UpdateItemRequest request = new UpdateItemRequest();
        request.setName("Updated Item");

        ItemResponse response = new ItemResponse();
        response.setItemId("ITEM-123");
        response.setName("Updated Item");

        when(itemService.updateItem(eq("ITEM-123"), any(UpdateItemRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/items/ITEM-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Item"));
    }

    @Test
    void testDeleteItem() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/items/ITEM-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetInventory() throws Exception {
        // Given
        InventoryResponse response = new InventoryResponse();
        response.setItemId("ITEM-123");
        response.setAvailableStock(50);
        response.setStatus("IN_STOCK");

        when(itemService.getInventory("ITEM-123")).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/items/ITEM-123/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("ITEM-123"))
                .andExpect(jsonPath("$.availableStock").value(50));
    }

    @Test
    void testCheckAvailability() throws Exception {
        // Given
        InventoryCheckResponse response = new InventoryCheckResponse();
        response.setItemId("ITEM-123");
        response.setAvailable(true);
        response.setAvailableQuantity(50);

        when(itemService.checkAvailability("ITEM-123", 10)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/items/ITEM-123/availability")
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void testUpdateInventory() throws Exception {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setQuantity(20);
        request.setOperation("ADD");

        InventoryResponse response = new InventoryResponse();
        response.setItemId("ITEM-123");
        response.setAvailableStock(70);

        when(itemService.updateInventory(eq("ITEM-123"), any(InventoryUpdateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/items/ITEM-123/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(70));
    }

    @Test
    void testReserveInventory() throws Exception {
        // Given
        ReserveInventoryRequest request = new ReserveInventoryRequest();
        request.setItemId("ITEM-123");
        request.setQuantity(5);

        when(itemService.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/items/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/items/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Item Service is running"));
    }
}


@ExtendWith(MockitoExtension.class)
class ItemServiceSimpleTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ItemEventProducer eventProducer;

    @InjectMocks
    private ItemService itemService;

    @Test
    void testCreateItem_Success() {
        // Given
        CreateItemRequest request = new CreateItemRequest();
        request.setName("Test Item");
        request.setUpc("123456789");
        request.setPrice(new BigDecimal("99.99"));
        request.setCategory("Electronics");
        request.setInitialStock(10);

        Item savedItem = new Item();
        savedItem.setItemId("ITEM-123");
        savedItem.setName("Test Item");

        when(itemRepository.findByUpc(anyString())).thenReturn(Optional.empty());
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(new Inventory());

        // When
        ItemResponse result = itemService.createItem(request);

        // Then
        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void testGetItemById_Success() {
        // Given
        Item item = new Item();
        item.setItemId("ITEM-123");
        item.setName("Test Item");
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        when(itemRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(item));

        // When
        ItemResponse result = itemService.getItemById("ITEM-123");

        // Then
        assertNotNull(result);
        assertEquals("ITEM-123", result.getItemId());
    }

    @Test
    void testGetItemById_NotFound() {
        // Given
        when(itemRepository.findByItemId("NOT-FOUND")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ItemNotFoundException.class, () -> itemService.getItemById("NOT-FOUND"));
    }

    @Test
    void testGetInventory_Success() {
        // Given
        Inventory inventory = new Inventory();
        inventory.setItemId("ITEM-123");
        inventory.setAvailableStock(50);
        inventory.setLastUpdated(LocalDateTime.now());

        when(inventoryRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(inventory));

        // When
        InventoryResponse result = itemService.getInventory("ITEM-123");

        // Then
        assertNotNull(result);
        assertEquals("ITEM-123", result.getItemId());
        assertEquals(50, result.getAvailableStock());
    }

    @Test
    void testCheckAvailability_Available() {
        // Given
        Inventory inventory = new Inventory();
        inventory.setItemId("ITEM-123");
        inventory.setAvailableStock(50);

        when(inventoryRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(inventory));

        // When
        InventoryCheckResponse result = itemService.checkAvailability("ITEM-123", 30);

        // Then
        assertNotNull(result);
        assertTrue(result.isAvailable());
        assertEquals(50, result.getAvailableQuantity());
    }

    @Test
    void testCheckAvailability_NotAvailable() {
        // Given
        Inventory inventory = new Inventory();
        inventory.setItemId("ITEM-123");
        inventory.setAvailableStock(20);

        when(inventoryRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(inventory));

        // When
        InventoryCheckResponse result = itemService.checkAvailability("ITEM-123", 30);

        // Then
        assertNotNull(result);
        assertFalse(result.isAvailable());
        assertEquals(20, result.getAvailableQuantity());
    }

    @Test
    void testReserveInventory_Success() {
        // Given
        ReserveInventoryRequest request = new ReserveInventoryRequest();
        request.setItemId("ITEM-123");
        request.setQuantity(10);

        Inventory inventory = new Inventory();
        inventory.setItemId("ITEM-123");
        inventory.setAvailableStock(50);
        inventory.setReservedStock(0);
        inventory.setMinStockLevel(5);

        when(inventoryRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        boolean result = itemService.reserveInventory(request);

        // Then
        assertTrue(result);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void testReserveInventory_InsufficientStock() {
        // Given
        ReserveInventoryRequest request = new ReserveInventoryRequest();
        request.setItemId("ITEM-123");
        request.setQuantity(100);

        Inventory inventory = new Inventory();
        inventory.setItemId("ITEM-123");
        inventory.setAvailableStock(50);

        when(inventoryRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(inventory));

        // When
        boolean result = itemService.reserveInventory(request);

        // Then
        assertFalse(result);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testUpdateItem_Success() {
        // Given
        UpdateItemRequest request = new UpdateItemRequest();
        request.setName("Updated Item");

        Item item = new Item();
        item.setItemId("ITEM-123");
        item.setName("Original Item");

        when(itemRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // When
        ItemResponse result = itemService.updateItem("ITEM-123", request);

        // Then
        assertNotNull(result);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void testDeleteItem_Success() {
        // Given
        Item item = new Item();
        item.setItemId("ITEM-123");

        when(itemRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(item));
        when(inventoryRepository.findByItemId("ITEM-123")).thenReturn(Optional.of(new Inventory()));

        // When
        itemService.deleteItem("ITEM-123");

        // Then
        verify(itemRepository).save(any(Item.class));
    }
}

public class UnitTests {


}
