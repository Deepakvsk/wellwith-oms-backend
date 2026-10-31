package com.wellwithlife.omsbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sku_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", unique = true, nullable = false)
    private String skuCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Column(name = "is_sample")
    private Boolean isSample = false;

    @Column(name = "is_active")
    private Boolean isActive = true;
}