package com.ecommerce.platform.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsResponse {
    private Long todayOrders;
    private Long weekOrders;
    private Long monthOrders;
    private Map<String, Long> ordersByStatus;
}
