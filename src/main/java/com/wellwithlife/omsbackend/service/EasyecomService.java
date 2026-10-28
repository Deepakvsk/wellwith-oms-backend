package com.wellwithlife.omsbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellwithlife.omsbackend.dto.EasyecomApiResponse;
import com.wellwithlife.omsbackend.dto.EasyecomOrderPayload;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.OrderFulfillment;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.OrderFulfillmentRepository;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EasyecomService {

    private final WebClient easyecomWebClient;
    private final OrderRepository orderRepository;
    private final OrderFulfillmentRepository fulfillmentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order pushOrderToEasyecom(Long orderId, User adminUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if ("SYNCED_TO_EASYECOM".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order is already synced to Easyecom!");
        }

        // 1. Map to Easyecom payload
        List<EasyecomOrderPayload.EasyecomItem> items = order.getItems().stream()
                .map(i -> EasyecomOrderPayload.EasyecomItem.builder()
                        .sku(i.getSkuCode())
                        .quantity(i.getQuantity())
                        .price(i.getSellingPrice())
                        .build())
                .collect(Collectors.toList());

        EasyecomOrderPayload payload = EasyecomOrderPayload.builder()
                .orderId(order.getReferenceCode())
                .orderType(order.getOrderType())
                .paymentMode(order.getOrderType().equalsIgnoreCase("COD") ? "COD" : "PREPAID")
                .totalAmount(order.getTotalAmount())
                .collectableAmount(order.getAmountToCollect())
                .customerName(order.getCustomerName())
                .customerPhone(order.getPhone())
                .customerEmail(order.getEmail())
                .customerAddress(order.getAddress())
                .city(order.getCity())
                .state(order.getState())
                .pincode(order.getPincode())
                .orderItems(items)
                .build();

        // 2. Fulfillment entity prepare/fetch
        OrderFulfillment fulfillment = order.getFulfillment();
        if (fulfillment == null) {
            fulfillment = OrderFulfillment.builder()
                    .order(order)
                    .deliveryStatus("PENDING")
                    .build();
            order.setFulfillment(fulfillment);
        }

        try {
            log.info("Sending order {} to Easyecom REST API...", order.getReferenceCode());

            // 3. Hit Easyecom Order Creation Endpoint
            EasyecomApiResponse response = easyecomWebClient.post()
                    .uri("/orders/create")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(EasyecomApiResponse.class)
                    .block();

            if (response != null && "success".equalsIgnoreCase(response.getStatus())) {
                order.setStatus("SYNCED_TO_EASYECOM");
                order.setApprovedBy(adminUser);

                fulfillment.setEasyecomOrderId(response.getEasyecomOrderId() != null ? response.getEasyecomOrderId() : order.getReferenceCode());
                fulfillment.setAwbNumber(response.getAwbNumber());
                fulfillment.setShippingPartner(response.getShippingPartner());
                fulfillment.setDeliveryLink(response.getTrackingUrl());
                fulfillment.setSyncedAt(LocalDateTime.now());
                fulfillment.setSyncErrorPayload(null);
            } else {
                order.setStatus("SYNC_FAILED");
                fulfillment.setSyncErrorPayload(response != null ? response.getMessage() : "Empty response from Easyecom");
            }
        } catch (Exception ex) {
            log.error("Failed to push order {} to Easyecom: {}", order.getReferenceCode(), ex.getMessage());
            order.setStatus("SYNC_FAILED");
            fulfillment.setSyncErrorPayload(ex.getMessage());
        }

        fulfillmentRepository.save(fulfillment);
        return orderRepository.save(order);
    }
}