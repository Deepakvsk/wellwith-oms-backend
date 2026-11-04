package com.wellwithlife.omsbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "reference_code", nullable = false, unique = true, length = 100)
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bde_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    // Full address string for fallback and UI display
    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    // Address Breakdown for EasyEcom V2 Dispatch
    @Column(name = "address_line_1", columnDefinition = "TEXT")
    private String addressLine1;

    @Column(name = "address_line_2", columnDefinition = "TEXT")
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(name = "country", length = 50)
    @Builder.Default
    private String country = "India";

    // --- Distinct Billing Address Fields ---
    @Column(name = "billing_name", length = 150)
    private String billingName;

    @Column(name = "billing_phone", length = 20)
    private String billingPhone;

    @Column(name = "billing_address_line_1", columnDefinition = "TEXT")
    private String billingAddressLine1;

    @Column(name = "billing_address_line_2", columnDefinition = "TEXT")
    private String billingAddressLine2;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_pincode", length = 10)
    private String billingPincode;

    // --- EasyEcom Package Dimensions & Weight ---
    @Column(name = "package_weight")
    @Builder.Default
    private Integer packageWeight = 500; // in grams

    @Column(name = "package_length")
    @Builder.Default
    private Integer packageLength = 10; // in cm

    @Column(name = "package_width")
    @Builder.Default
    private Integer packageWidth = 10; // in cm

    @Column(name = "package_height")
    @Builder.Default
    private Integer packageHeight = 10; // in cm

    // --- Compliance & EasyEcom Remarks ---
    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "remarks_1", length = 255)
    private String remarks1;

    @Column(name = "remarks_2", length = 255)
    private String remarks2;

    // --- Order Types & Statuses ---
    @Column(name = "order_type", nullable = false, length = 30)
    private String orderType; // PREPAID, COD, PARTIAL_COD

    @Column(name = "order_source", length = 50)
    @Builder.Default
    private String orderSource = "MANUAL";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING_APPROVAL";
    // PENDING_APPROVAL, APPROVED, SYNCED_TO_EASYECOM, STOCK_FAILED, SYNC_FAILED, REJECTED, CONFIRMED

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_to_collect", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountToCollect = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private OrderFulfillment fulfillment;

    // --- Other Charges ---
    @Column(name = "other_charges")
    @Builder.Default
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "other_charges_desc")
    private String otherChargesDescription;

    // --- Discount ---
    @Column(name = "discount_percentage")
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // --- Payment & Notes ---
    @Column(name = "payment_type")
    private String paymentType; // COD, PREPAID, PARTIAL_COD

    private String paymentGateway;
    private String paymentTransactionNumber;

    @Column(name = "payment_screenshot_url")
    private String paymentScreenshotUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_free_product")
    @Builder.Default
    private Boolean isFreeProduct = false;

    @Column(name = "line_total")
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING_APPROVAL";
        }
        // Auto-populate addressLine1 if only address was supplied
        if (this.addressLine1 == null || this.addressLine1.isBlank()) {
            this.addressLine1 = this.address;
        }
        // Fallback: If billing fields are empty, sync from shipping
        if (this.billingName == null || this.billingName.isBlank()) {
            this.billingName = this.customerName;
            this.billingPhone = this.phone;
            this.billingAddressLine1 = this.addressLine1;
            this.billingAddressLine2 = this.addressLine2;
            this.billingCity = this.city;
            this.billingState = this.state;
            this.billingPincode = this.pincode;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to keep bidirectional mapping in sync
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void setFulfillment(OrderFulfillment fulfillment) {
        this.fulfillment = fulfillment;
        if (fulfillment != null) {
            fulfillment.setOrder(this);
        }
    }
}