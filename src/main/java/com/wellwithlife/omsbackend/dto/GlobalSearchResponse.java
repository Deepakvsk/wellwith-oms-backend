package com.wellwithlife.omsbackend.dto;

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
public class GlobalSearchResponse {

    private String query;
    private String scope; // "GLOBAL" (Admin) or "PERSONAL" (BDE)
    private int totalOrdersFound;
    private BigDecimal totalRevenue;

    // Customer 360 Insights (Populated when query matches customer phone/name)
    private List<CustomerInsightDto> customers;

    // Matching Orders List
    private List<OrderResponse> orders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInsightDto {
        private String customerName;
        private String phone;
        private String email;
        private String city;
        private String state;
        private long totalOrders;
        private long deliveredOrders;
        private long cancelledOrRejectedOrders;
        private BigDecimal lifetimeValue;
        private BigDecimal pendingCodAmount;
    }
}