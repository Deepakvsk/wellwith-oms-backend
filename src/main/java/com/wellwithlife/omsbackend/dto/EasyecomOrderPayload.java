package com.wellwithlife.omsbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EasyecomOrderPayload {

    @JsonProperty("reference_code")
    private String referenceCode;

    @JsonProperty("order_date")
    private String orderDate; // Format: "yyyy-MM-dd HH:mm:ss"

    @JsonProperty("payment_mode")
    private String paymentMode; // "COD" or "Prepaid"

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("collectable_amount")
    private BigDecimal collectableAmount;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("contact_number")
    private String contactNumber;

    @JsonProperty("email")
    private String email;

    @JsonProperty("address_line_1")
    private String addressLine1;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("pin_code")
    private String pinCode;

    @JsonProperty("country")
    @Builder.Default
    private String country = "India";

    @JsonProperty("location_key")
    private String locationKey;

    @JsonProperty("order_items")
    private List<EasyecomItem> orderItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EasyecomItem {

        @JsonProperty("sku")
        private String sku;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("selling_price")
        private BigDecimal sellingPrice;

        @JsonProperty("discount")
        @Builder.Default
        private BigDecimal discount = BigDecimal.ZERO;

        @JsonProperty("tax_rate")
        @Builder.Default
        private BigDecimal taxRate = BigDecimal.ZERO;
    }
}