package com.wellwithlife.omsbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long pendingFulfillment;
    private long fulfilled;
    private long inTransit;
    private long delivered;
    private long rto;
    private long syncFailed;
}