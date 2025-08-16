package com.codebase.microservices.orderservice;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@UserDefinedType("order_item")
public class OrderItem {

    @Column("item_id")
    private String itemId;

    @Column("item_name")
    private String itemName;

    private String upc;
    private Integer quantity;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("total_price")
    private BigDecimal totalPrice;

    @Column("item_picture_url")
    private String itemPictureUrl;
}