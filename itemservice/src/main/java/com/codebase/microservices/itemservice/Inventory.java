package com.codebase.microservices.itemservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inventory")
public class Inventory {

    @Id
    private String id;

    @Indexed(unique = true)
    private String itemId;

    private Integer totalStock;

    private Integer availableStock;

    private Integer reservedStock;

    private Integer minStockLevel; // For reorder alerts

    private Integer maxStockLevel;

    private String warehouseLocation;

    private LocalDateTime lastRestocked;

    private LocalDateTime lastUpdated;

    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED
}