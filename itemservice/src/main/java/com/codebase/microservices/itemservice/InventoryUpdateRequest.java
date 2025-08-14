package com.codebase.microservices.itemservice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @NotBlank(message = "Operation type is required")
    @Pattern(regexp = "ADD|SUBTRACT|SET", message = "Operation must be ADD, SUBTRACT, or SET")
    private String operation; // ADD, SUBTRACT, SET

    private String reason;
    private String warehouseLocation;
}
