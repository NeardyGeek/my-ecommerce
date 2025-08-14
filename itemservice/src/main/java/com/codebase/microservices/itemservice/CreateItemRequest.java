package com.codebase.microservices.itemservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItemRequest {

    @NotBlank(message = "Item name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String brand;

    @NotBlank(message = "UPC is required")
    private String upc;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private String currency = "USD";

    private List<String> imageUrls;

    private Map<String, Object> specifications;

    private List<String> tags;

    private String sku;

    private Double weight;

    private Map<String, String> dimensions;

    // Initial inventory data
    @NotNull(message = "Initial stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer initialStock;

    private Integer minStockLevel = 10;

    private Integer maxStockLevel = 1000;

    private String warehouseLocation = "MAIN";
}


