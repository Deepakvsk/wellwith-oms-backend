package com.wellwithlife.omsbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class EasyecomOrderPayload {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("order_type")
    private String orderType; // Prepaid / COD

    @JsonProperty("payment_mode")
    private String paymentMode;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("collectable_amount")
    private BigDecimal collectableAmount;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_phone")
    private String customerPhone;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("customer_address")
    private String customerAddress;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("pincode")
    private String pincode;

    @JsonProperty("order_items")
    private List<EasyecomItem> orderItems;

    @Data
    @Builder
    public static class EasyecomItem {
        @JsonProperty("sku")
        private String sku;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("price")
        private BigDecimal price;
    }
}