package com.wellwithlife.omsbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long orderId;
    private String referenceCode;
    private String customerName;
    private String phone;
    private String email;
    private String city;
    private String state;
    private String pincode;
    private String orderType;
    private String notes;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal amountToCollect;
    private String createdByName;
    private String easyecomOrderId;
    private String awbNumber;
    private String shippingPartner;
    private String deliveryLink;
    private String deliveryStatus;
    private LocalDateTime createdAt;
    private List<ItemDto> items;

    @Data
    @Builder
    public static class ItemDto {
        private String skuCode;
        private String productName;
        private Integer quantity;
        private BigDecimal sellingPrice;
        private Boolean isFreeProduct;
    }
}