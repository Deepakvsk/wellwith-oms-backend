package com.wellwithlife.omsbackend.service;

import com.wellwithlife.omsbackend.dto.CreateOrderRequest;
import com.wellwithlife.omsbackend.dto.DashboardStatsResponse;
import com.wellwithlife.omsbackend.dto.OrderResponse;
import com.wellwithlife.omsbackend.dto.UpdateOrderRequest;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.OrderItem;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import com.wellwithlife.omsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        // 1. Resolve Full Address & Fallbacks
        String resolvedAddress = request.getFinalAddress();
        String resolvedAddress1 = (request.getAddressLine1() != null && !request.getAddressLine1().isBlank())
                ? request.getAddressLine1()
                : resolvedAddress;

        // 2. Resolve Notes / Remarks
        String resolvedNotes = (request.getNotes() != null && !request.getNotes().isBlank())
                ? request.getNotes()
                : request.getRemarks2();

        // 3. Billing Address Resolution (Sync with shipping if sameAsShipping is true)
        boolean isSame = Boolean.TRUE.equals(request.getSameAsShipping());
        String bName = isSame ? request.getCustomerName() : request.getBillingName();
        String bPhone = isSame ? request.getPhone() : request.getBillingPhone();
        String bAddr1 = isSame ? resolvedAddress1 : request.getBillingAddressLine1();
        String bAddr2 = isSame ? request.getAddressLine2() : request.getBillingAddressLine2();
        String bCity = isSame ? request.getCity() : request.getBillingCity();
        String bState = isSame ? request.getState() : request.getBillingState();
        String bPin = isSame ? request.getPincode() : request.getBillingPincode();

        // 4. Build Order Entity with all database fields
        Order order = Order.builder()
                .referenceCode(request.getReferenceCode())
                .createdBy(bdeUser)
                .customerName(request.getCustomerName())
                .phone(request.getPhone())
                .email(request.getEmail())

                // Shipping Address breakdown
                .address(resolvedAddress)
                .addressLine1(resolvedAddress1)
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry() != null ? request.getCountry() : "India")

                // Billing Address
                .billingName(bName)
                .billingPhone(bPhone)
                .billingAddressLine1(bAddr1)
                .billingAddressLine2(bAddr2)
                .billingCity(bCity)
                .billingState(bState)
                .billingPincode(bPin)

                // EasyEcom Package Specs
                .packageWeight(request.getPackageWeight() != null ? request.getPackageWeight() : 500)
                .packageLength(request.getPackageLength() != null ? request.getPackageLength() : 10)
                .packageWidth(request.getPackageWidth() != null ? request.getPackageWidth() : 10)
                .packageHeight(request.getPackageHeight() != null ? request.getPackageHeight() : 10)

                // Remarks & GST
                .gstNumber(request.getGstNumber())
                .remarks1(request.getRemarks1())
                .remarks2(request.getRemarks2())

                // Types & Status
                .orderType(request.getOrderType() != null ? request.getOrderType() : "PREPAID")
                .paymentType(request.getPaymentType() != null ? request.getPaymentType() : "COD")
                .paymentScreenshotUrl(request.getPaymentScreenshotUrl())
                .orderSource("MANUAL")
                .status("PENDING_APPROVAL")
                .notes(resolvedNotes)

                // Pricing, Discounts & Other Charges (Persist to DB)
                .totalAmount(request.getTotalAmount())
                .amountToCollect(request.getAmountToCollect() != null ? request.getAmountToCollect() : request.getTotalAmount())
                .otherCharges(request.getOtherCharges() != null ? request.getOtherCharges() : BigDecimal.ZERO)
                .otherChargesDescription(request.getOtherChargesDescription())
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO)
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .build();

        // 5. Line items mapping (Includes lineTotal & availableStock)
        if (request.getItems() != null) {
            request.getItems().forEach(itemDto -> {
                boolean isFree = Boolean.TRUE.equals(itemDto.getIsFreeProduct());
                BigDecimal unitPrice = isFree ? BigDecimal.ZERO : itemDto.getSellingPrice();
                int qty = itemDto.getQuantity() != null ? itemDto.getQuantity() : 1;

                BigDecimal calculatedLineTotal = itemDto.getLineTotal() != null
                        ? itemDto.getLineTotal()
                        : (isFree ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(qty)));

                OrderItem item = OrderItem.builder()
                        .skuCode(itemDto.getSkuCode())
                        .productName(itemDto.getProductName())
                        .quantity(qty)
                        .sellingPrice(unitPrice)
                        .lineTotal(calculatedLineTotal)
                        .isFreeProduct(isFree)
                        .availableStock(itemDto.getAvailableStock())
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
                .email(order.getEmail())

                // --- Address & Delivery ---
                .address(order.getAddress())
                .addressLine1(order.getAddressLine1() != null ? order.getAddressLine1() : order.getAddress())
                .addressLine2(order.getAddressLine2())
                .city(order.getCity())
                .state(order.getState())
                .pincode(order.getPincode())
                .country(order.getCountry())

                // --- Billing Address ---
                .billingName(order.getBillingName())
                .billingPhone(order.getBillingPhone())
                .billingAddressLine1(order.getBillingAddressLine1())
                .billingAddressLine2(order.getBillingAddressLine2())
                .billingCity(order.getBillingCity())
                .billingState(order.getBillingState())
                .billingPincode(order.getBillingPincode())

                // --- Order Type, Payment & Status ---
                .orderType(order.getOrderType())
                .paymentType(order.getPaymentType())
                .status(order.getStatus())
                .notes(order.getNotes())
                .gstNumber(order.getGstNumber())
                .remarks1(order.getRemarks1())
                .remarks2(order.getRemarks2())

                // --- Pricing, Discounts & Other Charges ---
                .totalAmount(order.getTotalAmount())
                .discountPercentage(order.getDiscountPercentage())
                .discountAmount(order.getDiscountAmount())
                .otherCharges(order.getOtherCharges())
                .otherChargesDescription(order.getOtherChargesDescription())
                .amountToCollect(order.getAmountToCollect())

                // --- Audit & Fulfillment Sync ---
                .createdByName(order.getCreatedBy() != null ? order.getCreatedBy().getName() : "N/A")
                .easyecomOrderId(order.getFulfillment() != null ? order.getFulfillment().getEasyecomOrderId() : null)
                .shippingPartner(order.getFulfillment() != null ? order.getFulfillment().getShippingPartner() : null)
                .awbNumber(order.getFulfillment() != null ? order.getFulfillment().getAwbNumber() : null)
                .deliveryLink(order.getFulfillment() != null ? order.getFulfillment().getDeliveryLink() : null)
                .deliveryStatus(order.getFulfillment() != null ? order.getFulfillment().getDeliveryStatus() : "PENDING")
                .syncErrorPayload(order.getFulfillment() != null ? order.getFulfillment().getSyncErrorPayload() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())

                // --- Line Items Mapping ---
                .items(order.getItems() != null ? order.getItems().stream().map(i -> OrderResponse.ItemDto.builder()
                        .skuCode(i.getSkuCode())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .sellingPrice(i.getSellingPrice())
                        .lineTotal(i.getLineTotal() != null ? i.getLineTotal() :
                                (i.getSellingPrice() != null && i.getQuantity() != null
                                        ? i.getSellingPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity()))
                                        : java.math.BigDecimal.ZERO))
                        .isFreeProduct(i.getIsFreeProduct())
                        .build()).collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }
    // 1. Dashboard KPI Cards Stats
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
                .pendingFulfillment(orderRepository.countByStatus("PENDING_APPROVAL"))
                .fulfilled(orderRepository.countByStatus("SYNCED_TO_EASYECOM"))
                .inTransit(orderRepository.countByDeliveryStatus("IN_TRANSIT"))
                .delivered(orderRepository.countByDeliveryStatus("DELIVERED"))
                .rto(orderRepository.countByDeliveryStatus("RTO"))
                .syncFailed(orderRepository.countByStatus("SYNC_FAILED"))
                .build();
    }

    // 2. All Orders with Filter and Pagination (Main Table)
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderResponse> getAllOrdersFiltered(
            String status, Long bdeId, String deliveryStatus, String search, org.springframework.data.domain.Pageable pageable
    ) {
        return orderRepository.findOrdersWithFilters(status, bdeId, deliveryStatus, search, pageable)
                .map(this::mapToOrderResponse);
    }

    // 3. Admin Order Verification & Update
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if ("SYNCED_TO_EASYECOM".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Cannot edit order once synced to Easyecom!");
        }

        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setEmail(request.getEmail());
        order.setAddress(request.getAddress());
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setPincode(request.getPincode());
        if (request.getOrderType() != null) {
            order.setOrderType(request.getOrderType());
        }

        return mapToOrderResponse(orderRepository.save(order));
    }

    // 4. Admin Reject Order
    @Transactional
    public OrderResponse rejectOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        order.setStatus("REJECTED");
        return mapToOrderResponse(orderRepository.save(order));
    }

}