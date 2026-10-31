package com.wellwithlife.omsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {

    @NotBlank(message = "SKU Code is required")
    private String skuCode;

    private String productName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "Selling price is required")
    private BigDecimal sellingPrice;

    private BigDecimal lineTotal;

    private Boolean isFreeProduct = false;
}