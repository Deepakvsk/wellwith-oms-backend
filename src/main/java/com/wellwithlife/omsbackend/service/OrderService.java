package com.wellwithlife.omsbackend.service;

import com.wellwithlife.omsbackend.dto.CreateOrderRequest;
import com.wellwithlife.omsbackend.dto.OrderResponse;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.OrderItem;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import com.wellwithlife.omsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        if (orderRepository.findByReferenceCode(request.getReferenceCode()).isPresent()) {
            throw new IllegalArgumentException("Order with Reference Code " + request.getReferenceCode() + " already exists.");
        }

        User bdeUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Order order = Order.builder()
                .referenceCode(request.getReferenceCode())
                .createdBy(bdeUser)
                .customerName(request.getCustomerName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .orderType(request.getOrderType())
                .orderSource("MANUAL")
                .status("PENDING_APPROVAL")
                .totalAmount(request.getTotalAmount())
                .amountToCollect(request.getAmountToCollect() != null ? request.getAmountToCollect() : request.getTotalAmount())
                .build();

        if (request.getItems() != null) {
            request.getItems().forEach(itemDto -> {
                OrderItem item = OrderItem.builder()
                        .skuCode(itemDto.getSkuCode())
                        .productName(itemDto.getProductName())
                        .quantity(itemDto.getQuantity())
                        .sellingPrice(itemDto.getSellingPrice())
                        .isFreeProduct(itemDto.getIsFreeProduct())
                        .build();
                order.addItem(item);
            });
        }

        Order saved = orderRepository.save(order);
        return mapToOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingOrders() {
        return orderRepository.findByStatusOrderByCreatedAtDesc("PENDING_APPROVAL")
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByBde(Long bdeId) {
        return orderRepository.findByCreatedBy_UserIdOrderByCreatedAtDesc(bdeId)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .referenceCode(order.getReferenceCode())
                .customerName(order.getCustomerName())
                .phone(order.getPhone())
                .city(order.getCity())
                .state(order.getState())
                .pincode(order.getPincode())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .amountToCollect(order.getAmountToCollect())
                .createdByName(order.getCreatedBy() != null ? order.getCreatedBy().getName() : "N/A")
                .easyecomOrderId(order.getFulfillment() != null ? order.getFulfillment().getEasyecomOrderId() : null)
                .awbNumber(order.getFulfillment() != null ? order.getFulfillment().getAwbNumber() : null)
                .deliveryLink(order.getFulfillment() != null ? order.getFulfillment().getDeliveryLink() : null)
                .deliveryStatus(order.getFulfillment() != null ? order.getFulfillment().getDeliveryStatus() : "PENDING")
                .createdAt(order.getCreatedAt())
                .items(order.getItems() != null ? order.getItems().stream().map(i -> OrderResponse.ItemDto.builder()
                        .skuCode(i.getSkuCode())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .sellingPrice(i.getSellingPrice())
                        .isFreeProduct(i.getIsFreeProduct())
                        .build()).collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }
}