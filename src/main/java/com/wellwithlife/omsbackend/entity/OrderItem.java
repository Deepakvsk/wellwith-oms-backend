package com.wellwithlife.omsbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    @Column(name = "product_name")
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    // 1. Line Total (Qty * Price)
    @Column(name = "line_total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    // 2. Free Sample / Product Flag
    @Column(name = "is_free_product")
    @Builder.Default
    private Boolean isFreeProduct = false;

    // 3. Snapshot of inventory at time of order creation
    @Column(name = "available_stock")
    private Integer availableStock;

    @PrePersist
    @PreUpdate
    protected void calculateLineTotal() {
        if (Boolean.TRUE.equals(this.isFreeProduct)) {
            this.lineTotal = BigDecimal.ZERO;
            this.sellingPrice = BigDecimal.ZERO;
        } else if (this.sellingPrice != null && this.quantity != null) {
            this.lineTotal = this.sellingPrice.multiply(BigDecimal.valueOf(this.quantity));
        } else {
            this.lineTotal = BigDecimal.ZERO;
        }
    }
}