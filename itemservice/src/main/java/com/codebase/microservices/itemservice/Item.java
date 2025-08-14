package com.codebase.microservices.itemservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "items")
public class Item {

    @Id
    private String id;

    @Indexed(unique = true)
    private String itemId;

    @Indexed
    private String name;

    private String description;

    @Indexed
    private String category;

    private String brand;

    @Indexed(unique = true)
    private String upc; // Universal Product Code

    private BigDecimal price;

    private String currency = "USD";

    private List<String> imageUrls;

    private Map<String, Object> specifications; // Flexible metadata storage

    private String status; // ACTIVE, INACTIVE, DISCONTINUED

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // SEO and search related fields
    private List<String> tags;

    private String sku; // Stock Keeping Unit

    private Double weight;

    private Map<String, String> dimensions; // length, width, height
}