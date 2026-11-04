package com.wellwithlife.omsbackend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderRequest {

    // 1. Root Identity & Reference
    @NotBlank(message = "Order reference code is required")
    private String referenceCode;

    // 2. Customer & Shipping Fields
    @JsonAlias({"name", "recipientName"})
    private String customerName;

    @JsonAlias({"contact", "mobile", "phoneNumber"})
    private String phone;

    private String email;

    // Full address string from frontend
    private String address;

    private String addressLine1;
    private String addressLine2;

    private String city;
    private String state;

    @JsonAlias({"postalCode", "pin"})
    private String pincode;

    @Builder.Default
    private String country = "India";

    // 3. Distinct Billing Address Fields
    private String billingName;
    private String billingPhone;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPincode;

    @Builder.Default
    private Boolean sameAsShipping = true;

    // 4. EasyEcom Specific Attributes & Dimensions
    private String gstNumber;
    private String remarks1;
    private String remarks2;

    @Builder.Default
    private Integer packageWeight = 500;
    @Builder.Default
    private Integer packageLength = 10;
    @Builder.Default
    private Integer packageWidth = 10;
    @Builder.Default
    private Integer packageHeight = 10;

    // 5. Order & Payment
    @Builder.Default
    private String orderType = "PREPAID";

    @Builder.Default
    private String paymentType = "COD";

    private String paymentGateway;
    private String paymentTransactionNumber;
    private String paymentScreenshotUrl;

    // 6. Pricing, Discounts & Charges
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal otherCharges;

    @JsonAlias({"otherChargesDesc", "other_charges_desc"})
    private String otherChargesDescription;

    private BigDecimal amountToCollect;
    private String notes;

    // 7. Entity Root Calculations
    @Builder.Default
    private Boolean isFreeProduct = false;

    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    // 8. Line Items
    @NotEmpty(message = "Order must contain at least one product")
    @Valid
    private List<OrderItemDto> items;

    // Fallback Helpers
    public String getCustomerName() {
        return (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
    }

    public String getFinalAddress() {
        if (address != null && !address.isBlank()) {
            return address;
        }
        if (addressLine1 != null && !addressLine1.isBlank()) {
            return (addressLine2 != null && !addressLine2.isBlank())
                    ? addressLine1 + ", " + addressLine2
                    : addressLine1;
        }
        return "";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemDto {

        @JsonAlias({"sku", "sku_code"})
        private String skuCode;

        private String productName;

        @Builder.Default
        private Integer quantity = 1;

        @JsonAlias({"price", "unitPrice"})
        private BigDecimal sellingPrice;

        private BigDecimal lineTotal;

        @Builder.Default
        private Boolean isFreeProduct = false;

        private Integer availableStock;

        public String getSkuCode() {
            return skuCode;
        }

        public BigDecimal getSellingPrice() {
            return sellingPrice != null ? sellingPrice : BigDecimal.ZERO;
        }
    }
}