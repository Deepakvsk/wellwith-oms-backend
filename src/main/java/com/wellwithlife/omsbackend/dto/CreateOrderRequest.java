package com.wellwithlife.omsbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "Reference code / Order ID is required")
    private String referenceCode;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    @NotBlank(message = "Order type (PREPAID/COD) is required")
    private String orderType;

    // --- New Fields: Other Charges ---
    private BigDecimal otherCharges = BigDecimal.ZERO;

    private String otherChargesDescription;

    // --- New Fields: Discount ---
    @DecimalMin(value = "0.0", message = "Discount percentage cannot be negative")
    @DecimalMax(value = "10.0", message = "Maximum allowed discount is 10%")
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    // --- New Fields: Payment Details & Notes ---
    private String paymentType; // COD, UPI, NETBANKING, CARD

    private String paymentScreenshotUrl;

    private String notes;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    private BigDecimal amountToCollect;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;
}