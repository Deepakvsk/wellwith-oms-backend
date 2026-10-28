package com.wellwithlife.omsbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_fulfillment_easyecom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fulfillment_id")
    private Long fulfillmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "easyecom_order_id", length = 100)
    private String easyecomOrderId;

    @Column(name = "awb_number", length = 100)
    private String awbNumber;

    @Column(name = "shipping_partner", length = 100)
    private String shippingPartner;

    @Column(name = "delivery_link", columnDefinition = "TEXT")
    private String deliveryLink;

    @Column(name = "delivery_status", length = 50)
    @Builder.Default
    private String deliveryStatus = "PENDING";

    @Column(name = "sync_error_payload", columnDefinition = "TEXT")
    private String syncErrorPayload;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}