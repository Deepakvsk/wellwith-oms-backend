package com.wellwithlife.omsbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.OrderFulfillment;
import com.wellwithlife.omsbackend.repository.OrderFulfillmentRepository;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks/easyecom")
@RequiredArgsConstructor
@Slf4j
public class EasyecomWebhookController {

    private final OrderRepository orderRepository;
    private final OrderFulfillmentRepository fulfillmentRepository;

    /**
     * EasyEcom Shipment / AWB Sync Webhook
     */
    @PostMapping("/shipment-status")
    @Transactional
    public ResponseEntity<String> handleShipmentWebhook(@RequestBody JsonNode payload) {
        log.info("Received EasyEcom shipment webhook payload: {}", payload);

        try {
            // EasyEcom webhook payload se keys extract karein
            // (EasyEcom alag-alag events me order_number, invoice_id, awb_number bhejta hai)
            String orderNumber = payload.path("order_number").asText(
                    payload.path("orderNumber").asText(payload.path("reference_code").asText(null))
            );

            if (orderNumber == null || orderNumber.isBlank()) {
                log.warn("Webhook received without a valid order number. Skipping...");
                return ResponseEntity.ok("IGNORED_NO_ORDER_NUMBER");
            }

            Optional<Order> orderOpt = orderRepository.findByReferenceCode(orderNumber);
            if (orderOpt.isEmpty()) {
                log.warn("Order not found in OMS for reference code: {}", orderNumber);
                return ResponseEntity.ok("ORDER_NOT_FOUND");
            }

            Order order = orderOpt.get();
            OrderFulfillment fulfillment = order.getFulfillment();
            if (fulfillment == null) {
                fulfillment = OrderFulfillment.builder()
                        .order(order)
                        .deliveryStatus("PENDING")
                        .build();
                order.setFulfillment(fulfillment);
            }

            // AWB details extract karein
            String awb = payload.path("awb_number").asText(
                    payload.path("awbNumber").asText(payload.path("tracking_number").asText(null))
            );
            String courier = payload.path("carrier_name").asText(
                    payload.path("courier_name").asText(payload.path("shippingPartner").asText(null))
            );
            String trackingUrl = payload.path("tracking_url").asText(
                    payload.path("trackingUrl").asText(null)
            );
            String shipmentStatus = payload.path("status").asText(
                    payload.path("current_status").asText("IN_TRANSIT")
            );

            if (awb != null && !awb.isBlank()) {
                fulfillment.setAwbNumber(awb);
            }
            if (courier != null && !courier.isBlank()) {
                fulfillment.setShippingPartner(courier);
            }
            if (trackingUrl != null && !trackingUrl.isBlank()) {
                fulfillment.setDeliveryLink(trackingUrl);
            }

            // Delivery Status Mapping
            if (shipmentStatus.toLowerCase().contains("delivered")) {
                fulfillment.setDeliveryStatus("DELIVERED");
            } else if (shipmentStatus.toLowerCase().contains("rto")) {
                fulfillment.setDeliveryStatus("RTO");
            } else if (awb != null && !awb.isBlank()) {
                fulfillment.setDeliveryStatus("IN_TRANSIT");
            }

            fulfillmentRepository.save(fulfillment);
            orderRepository.save(order);

            log.info("Successfully updated fulfillment for order: {}, AWB: {}, Courier: {}", orderNumber, awb, courier);
            return ResponseEntity.ok("SUCCESS");

        } catch (Exception ex) {
            log.error("Failed to process EasyEcom webhook: {}", ex.getMessage());
            return ResponseEntity.internalServerError().body("ERROR: " + ex.getMessage());
        }
    }
}