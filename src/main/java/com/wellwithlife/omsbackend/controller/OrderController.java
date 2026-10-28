package com.wellwithlife.omsbackend.controller;

import com.wellwithlife.omsbackend.dto.CreateOrderRequest;
import com.wellwithlife.omsbackend.dto.OrderResponse;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.User;
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

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final EasyecomService easyecomService;
    private final UserRepository userRepository;

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

        return ResponseEntity.ok(orderService.getOrdersByBde(currentUser.getUserId()));
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
}