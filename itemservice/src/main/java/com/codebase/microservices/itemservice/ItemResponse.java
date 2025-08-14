package com.codebase.microservices.itemservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {
    private String id;
    private String itemId;
    private String name;
    private String description;
    private String category;
    private String brand;
    private String upc;
    private BigDecimal price;
    private String currency;
    private List<String> imageUrls;
    private Map<String, Object> specifications;
    private String status;
    private List<String> tags;
    private String sku;
    private Double weight;
    private Map<String, String> dimensions;
    private String createdAt;
    private String updatedAt;
}
