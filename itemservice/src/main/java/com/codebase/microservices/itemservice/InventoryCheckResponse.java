package com.codebase.microservices.itemservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResponse {
    private String itemId;
    private boolean available;
    private Integer availableQuantity;
    private String message;
}
