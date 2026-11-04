package com.wellwithlife.omsbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String referenceCode;
    private String customerName;
    private String phone;
    private String email;

    // --- Address Fields ---
    private String address;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;

    // --- Billing Details ---
    private String billingName;
    private String billingPhone;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPincode;

    // --- Order & Payment Types ---
    private String orderType;        // PREPAID, COD
    private String paymentType;      // COD, UPI, NETBANKING
    private String notes;
    private String status;
    private String gstNumber;
    private String remarks1;
    private String remarks2;

    // --- Pricing & Charges ---
    private BigDecimal totalAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal otherCharges;
    private String otherChargesDescription;
    private BigDecimal amountToCollect;

    // --- Tracking & Fulfillment ---
    private String createdByName;
    private String easyecomOrderId;
    private String awbNumber;
    private String shippingPartner;
    private String deliveryLink;
    private String deliveryStatus;
    private String syncErrorPayload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Line Items ---
    private List<ItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private String skuCode;
        private String productName;
        private Integer quantity;
        private BigDecimal sellingPrice;
        private BigDecimal lineTotal;
        private Boolean isFreeProduct;
    }
}