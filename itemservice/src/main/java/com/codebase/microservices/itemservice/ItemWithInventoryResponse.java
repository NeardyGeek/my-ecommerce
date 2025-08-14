package com.codebase.microservices.itemservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemWithInventoryResponse {
    private ItemResponse item;
    private InventoryResponse inventory;
}
