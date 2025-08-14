package com.codebase.microservices.itemservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private String itemId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer reservedStock;
    private String status;
    private String warehouseLocation;
    private String lastUpdated;
}
