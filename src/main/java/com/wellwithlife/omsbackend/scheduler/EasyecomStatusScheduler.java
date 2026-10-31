package com.wellwithlife.omsbackend.scheduler;

import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.OrderFulfillment;
import com.wellwithlife.omsbackend.repository.OrderFulfillmentRepository;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import com.wellwithlife.omsbackend.service.EasyecomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EasyecomStatusScheduler {

    private final OrderRepository orderRepository;
    private final OrderFulfillmentRepository fulfillmentRepository;
    private final EasyecomService easyecomService;

    // Har 5 minute (300,000 ms) me background me execute hoga
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void syncPendingOrdersWithEasyecom() {
        // Sirf wahi orders uthayenge jo push ho chuke hain par final status (CONFIRMED / STOCK_FAILED) nahi mila
        List<Order> unconfirmedOrders = orderRepository.findOrdersPendingEasyecomSync();
        if (unconfirmedOrders == null || unconfirmedOrders.isEmpty()) {
            return;
        }

        log.info("EasyecomStatusScheduler: Checking live status for {} orders...", unconfirmedOrders.size());

        for (Order order : unconfirmedOrders) {
            try {
                Map<String, Object> liveStatus = easyecomService.checkLiveOrderStatus(order.getReferenceCode());
                if (liveStatus == null) {
                    continue;
                }

                String omsStatus = (String) liveStatus.get("omsStatus");

                // Check fail hone par ya response process me hone par database status corrupt na ho
                if ("CHECK_FAILED".equals(omsStatus) || "IN_PROCESS".equals(omsStatus)) {
                    log.debug("Order {} is still in process or status check failed. Skipping...", order.getReferenceCode());
                    continue;
                }

                OrderFulfillment fulfillment = order.getFulfillment();
                if (fulfillment == null) {
                    fulfillment = OrderFulfillment.builder()
                            .order(order)
                            .deliveryStatus("PENDING")
                            .build();
                    order.setFulfillment(fulfillment);
                }

                // 1. Stock / Inventory Failure Handling
                if ("STOCK_FAILED".equals(omsStatus)) {
                    order.setStatus("STOCK_FAILED");
                    fulfillment.setSyncErrorPayload("EasyEcom: Out of stock / Inventory unassigned");
                    fulfillmentRepository.save(fulfillment);
                    orderRepository.save(order);
                    log.warn("Order {} moved to STOCK_FAILED on EasyEcom", order.getReferenceCode());

                    // 2. Confirmed / Dispatched Handling
                } else if ("CONFIRMED".equals(omsStatus) || "CONFIRMED_DISPATCHED".equals(omsStatus)) {
                    order.setStatus("CONFIRMED");

                    // EasyEcom V2 API response fields
                    String awb = (String) liveStatus.get("awbNumber");
                    String carrier = (String) liveStatus.get("courier");
                    String trackingUrl = (String) liveStatus.get("trackingUrl");
                    String labelUrl = (String) liveStatus.get("labelUrl");

                    // AWB update
                    if (awb != null && !awb.isBlank()) {
                        fulfillment.setAwbNumber(awb);
                    }

                    // Carrier / Shipping Partner update
                    if (carrier != null && !carrier.isBlank()) {
                        fulfillment.setShippingPartner(carrier);
                    }

                    // Tracking / Label Link priority
                    if (trackingUrl != null && !trackingUrl.isBlank()) {
                        fulfillment.setDeliveryLink(trackingUrl);
                    } else if (labelUrl != null && !labelUrl.isBlank()) {
                        fulfillment.setDeliveryLink(labelUrl);
                    }

                    // Delivery status update if dispatched/manifested
                    if ("CONFIRMED_DISPATCHED".equals(omsStatus)) {
                        fulfillment.setDeliveryStatus("IN_TRANSIT");
                    }

                    fulfillment.setSyncErrorPayload(null);
                    fulfillmentRepository.save(fulfillment);
                    orderRepository.save(order);

                    log.info("Order {} successfully confirmed on EasyEcom. AWB: {}, Courier: {}",
                            order.getReferenceCode(), awb, carrier);
                }

            } catch (Exception ex) {
                log.error("Error updating EasyEcom status for order {}: {}", order.getReferenceCode(), ex.getMessage());
            }
        }
    }
}