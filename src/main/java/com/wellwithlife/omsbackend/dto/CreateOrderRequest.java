package com.wellwithlife.omsbackend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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

    // 2. Customer & Shipping Fields (Direct flat getters expected by OrderService)
    @JsonAlias({"name"})
    private String customerName;

    @JsonAlias({"contact", "mobile"})
    private String phone;

    private String email;

    @JsonAlias({"addressLine1"})
    private String address;

    private String addressLine1;
    private String addressLine2;

    private String city;
    private String state;

    @JsonAlias({"postalCode"})
    private String pincode;

    private String country;

    // 3. Billing Address Fields (Optional)
    private String billingName;
    private String billingPhone;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPincode;

    @Builder.Default
    private Boolean sameAsShipping = true;

    // 4. EasyEcom Specific Attributes
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

    // 5. Order & Payment Enums
    @Builder.Default
    private String orderType = "PREPAID";

    @Builder.Default
    private String paymentType = "COD";

    private String paymentGateway;
    private String paymentTransactionNumber;

    // 6. Pricing & Calculations
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal shippingCharges;
    private BigDecimal otherCharges;
    private String otherChargesDescription;
    private BigDecimal amountToCollect;

    private String notes;

    // 7. Line Items (Supports both flat item list and nested item list)
    @NotEmpty(message = "Order must contain at least one product")
    @Valid
    private List<OrderItemDto> items;

    // Backward-compatible fallback helpers if nested address object is received
    public String getCustomerName() {
        return (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
    }

    public String getAddress() {
        if (address != null && !address.isBlank()) {
            return address;
        }
        if (addressLine1 != null && !addressLine1.isBlank()) {
            return addressLine2 != null && !addressLine2.isBlank()
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

        // Fallback accessors matching both old and new conventions
        public String getSkuCode() {
            return skuCode;
        }

        public BigDecimal getSellingPrice() {
            return sellingPrice != null ? sellingPrice : BigDecimal.ZERO;
        }
    }
}