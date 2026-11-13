package com.wellwithlife.omsbackend.controller;

import com.wellwithlife.omsbackend.dto.CreateOrderRequest;
import com.wellwithlife.omsbackend.dto.DashboardStatsResponse;
import com.wellwithlife.omsbackend.dto.OrderResponse;
import com.wellwithlife.omsbackend.dto.UpdateOrderRequest;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import com.wellwithlife.omsbackend.repository.UserRepository;
import com.wellwithlife.omsbackend.service.EasyecomService;
import com.wellwithlife.omsbackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final EasyecomService easyecomService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        OrderResponse response = orderService.createOrder(request, currentUser.getUserId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<OrderResponse>> getPendingOrders() {
        return ResponseEntity.ok(orderService.getPendingOrders());
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if ("TEAM_LEADER".equalsIgnoreCase(currentUser.getRole())) {
            return ResponseEntity.ok(orderService.getOrdersForTeamLead(currentUser.getUserId()));
        }
        return ResponseEntity.ok(orderService.getOrdersByBde(currentUser.getUserId()));
    }

    @GetMapping("/team-orders")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<OrderResponse>> getTeamOrders(Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if ("TEAM_LEADER".equalsIgnoreCase(currentUser.getRole())) {
            return ResponseEntity.ok(orderService.getOrdersForTeamLead(currentUser.getUserId()));
        }
        return ResponseEntity.ok(orderRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Order::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(orderService::mapToOrderResponse)
                .toList());
    }

    @PostMapping("/{orderId}/push-easyecom")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<OrderResponse> pushToEasyecom(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminEmail));

        Order syncedOrder = easyecomService.pushOrderToEasyecom(orderId, admin);
        return ResponseEntity.ok(orderService.mapToOrderResponse(syncedOrder));
    }

    // 1. Dashboard Top KPI Stats
    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(orderService.getDashboardStats());
    }

    // 2. Main Fulfillment Table (Search, Filter & Pagination)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long bdeId,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getAllOrdersFiltered(status, bdeId, deliveryStatus, search, pageable));
    }

    // 3. Edit Order Details (Pre-Sync Check Guard)
    @PutMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'BDE')")
    public ResponseEntity<?> updateOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> updates
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        // Guard: Synced ya Confirmed order edit nahi ho sakta
        if ("SYNCED_TO_EASYECOM".equalsIgnoreCase(order.getStatus()) || "CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Synced ya Confirmed orders edit nahi kiye ja sakte!"));
        }

        if (updates.containsKey("customerName")) order.setCustomerName((String) updates.get("customerName"));
        if (updates.containsKey("phone")) order.setPhone((String) updates.get("phone"));
        if (updates.containsKey("email")) order.setEmail((String) updates.get("email"));
        if (updates.containsKey("address")) order.setAddress((String) updates.get("address"));
        if (updates.containsKey("city")) order.setCity((String) updates.get("city"));
        if (updates.containsKey("state")) order.setState((String) updates.get("state"));
        if (updates.containsKey("pincode")) order.setPincode((String) updates.get("pincode"));
        if (updates.containsKey("paymentType")) order.setPaymentType((String) updates.get("paymentType"));
        if (updates.containsKey("notes")) order.setNotes((String) updates.get("notes"));

        Order savedOrder = orderRepository.save(order);
        return ResponseEntity.ok(orderService.mapToOrderResponse(savedOrder));
    }

    // 4. Reject Order
    @PostMapping("/{orderId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "Rejected by Admin") String reason
    ) {
        return ResponseEntity.ok(orderService.rejectOrder(orderId, reason));
    }

    // 5. Verify live EasyEcom status on demand
    @GetMapping("/{orderId}/verify-easyecom-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'BDE')")
    public ResponseEntity<?> verifyOrderStatus(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Map<String, Object> liveStatus = easyecomService.checkLiveOrderStatus(order.getReferenceCode());

        String omsStatus = (String) liveStatus.get("omsStatus");
        if ("FAILED_OUT_OF_STOCK".equals(omsStatus) || "STOCK_FAILED".equals(omsStatus)) {
            order.setStatus("STOCK_FAILED");
            if (order.getFulfillment() != null) {
                order.getFulfillment().setSyncErrorPayload("EasyEcom: Inventory Assigned Failed / Out of stock");
            }
        } else if ("CONFIRMED".equals(omsStatus) || "CONFIRMED_DISPATCHED".equals(omsStatus)) {
            order.setStatus("CONFIRMED");
        }

        orderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "referenceCode", order.getReferenceCode(),
                "liveStatus", liveStatus,
                "currentOmsStatus", order.getStatus()
        ));
    }

    // 6. Get EasyEcom live products master
    @GetMapping("/easyecom/products")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'BDE')")
    public ResponseEntity<?> getEasyecomProducts() {
        return ResponseEntity.ok(easyecomService.fetchProductMaster());
    }

}