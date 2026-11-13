package com.wellwithlife.omsbackend.service;

import com.wellwithlife.omsbackend.dto.GlobalSearchResponse;
import com.wellwithlife.omsbackend.dto.OrderResponse;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import com.wellwithlife.omsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public GlobalSearchResponse executeSearch(String rawQuery, Authentication authentication) {
        String query = rawQuery != null ? rawQuery.trim().toLowerCase() : "";
        if (query.isEmpty()) {
            return GlobalSearchResponse.builder()
                    .query("")
                    .scope("NONE")
                    .totalOrdersFound(0)
                    .totalRevenue(BigDecimal.ZERO)
                    .customers(Collections.emptyList())
                    .orders(Collections.emptyList())
                    .build();
        }

        // 1. Safely resolve current logged-in User from Authentication
        User currentUser = resolveUser(authentication);
        String role = (currentUser != null && currentUser.getRole() != null)
                ? currentUser.getRole().toUpperCase()
                : "BDE";

        boolean isAdmin = role.contains("SUPERADMIN") || role.contains("ADMIN");
        boolean isTeamLead = role.contains("TEAM_LEADER");

        // 2. Role-Based Data Scoping (GLOBAL vs TEAM vs PERSONAL)
        List<Order> allAccessibleOrders;
        if (isAdmin || currentUser == null) {
            allAccessibleOrders = orderRepository.findAll();
        } else if (isTeamLead) {
            Long tlId = currentUser.getUserId();
            Set<Long> teamUserIds = userRepository.findAll().stream()
                    .filter(u -> Objects.equals(u.getUserId(), tlId) ||
                            (u.getTeamLead() != null && Objects.equals(u.getTeamLead().getUserId(), tlId)))
                    .map(User::getUserId)
                    .collect(Collectors.toSet());

            allAccessibleOrders = orderRepository.findAll().stream()
                    .filter(o -> o.getCreatedBy() != null && teamUserIds.contains(o.getCreatedBy().getUserId()))
                    .collect(Collectors.toList());
        } else {
            Long currentUserId = currentUser.getUserId();
            allAccessibleOrders = orderRepository.findAll().stream()
                    .filter(o -> o.getCreatedBy() != null && Objects.equals(o.getCreatedBy().getUserId(), currentUserId))
                    .collect(Collectors.toList());
        }

        // 3. Filter matching orders & sort newest first
        List<Order> matchedOrders = allAccessibleOrders.stream()
                .filter(o -> matchesQuery(o, query))
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // 4. Build Customer 360 Profiles grouped by Phone
        Map<String, List<Order>> ordersByPhone = matchedOrders.stream()
                .filter(o -> o.getPhone() != null && !o.getPhone().isBlank())
                .collect(Collectors.groupingBy(Order::getPhone, LinkedHashMap::new, Collectors.toList()));

        List<GlobalSearchResponse.CustomerInsightDto> customerProfiles = new ArrayList<>();

        for (Map.Entry<String, List<Order>> entry : ordersByPhone.entrySet()) {
            List<Order> custOrders = entry.getValue();
            Order latest = custOrders.get(0);

            long delivered = custOrders.stream()
                    .filter(o -> o.getFulfillment() != null
                            && o.getFulfillment().getDeliveryStatus() != null
                            && o.getFulfillment().getDeliveryStatus().toLowerCase().contains("delivered")
                            && !o.getFulfillment().getDeliveryStatus().toLowerCase().contains("undelivered"))
                    .count();

            long rejectedOrCancelled = custOrders.stream()
                    .filter(o -> "REJECTED".equalsIgnoreCase(o.getStatus()) || "CANCELLED".equalsIgnoreCase(o.getStatus()))
                    .count();

            BigDecimal ltv = custOrders.stream()
                    .filter(o -> !"REJECTED".equalsIgnoreCase(o.getStatus()) && !"CANCELLED".equalsIgnoreCase(o.getStatus()))
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pendingCod = custOrders.stream()
                    .filter(o -> !"REJECTED".equalsIgnoreCase(o.getStatus()) && !"CANCELLED".equalsIgnoreCase(o.getStatus()))
                    .map(o -> o.getAmountToCollect() != null ? o.getAmountToCollect() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            customerProfiles.add(GlobalSearchResponse.CustomerInsightDto.builder()
                    .customerName(latest.getCustomerName())
                    .phone(entry.getKey())
                    .email(latest.getEmail())
                    .city(latest.getCity())
                    .state(latest.getState())
                    .totalOrders(custOrders.size())
                    .deliveredOrders(delivered)
                    .cancelledOrRejectedOrders(rejectedOrCancelled)
                    .lifetimeValue(ltv)
                    .pendingCodAmount(pendingCod)
                    .build());
        }

        BigDecimal totalRev = matchedOrders.stream()
                .filter(o -> !"REJECTED".equalsIgnoreCase(o.getStatus()) && !"CANCELLED".equalsIgnoreCase(o.getStatus()))
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderResponse> orderDtos = matchedOrders.stream()
                .map(orderService::mapToOrderResponse)
                .collect(Collectors.toList());

        String resolvedScope = isAdmin ? "GLOBAL" : (isTeamLead ? "TEAM" : "PERSONAL");

        return GlobalSearchResponse.builder()
                .query(rawQuery)
                .scope(resolvedScope)
                .totalOrdersFound(orderDtos.size())
                .totalRevenue(totalRev)
                .customers(customerProfiles)
                .orders(orderDtos)
                .build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private boolean matchesQuery(Order o, String q) {
        if (o.getReferenceCode() != null && o.getReferenceCode().toLowerCase().contains(q)) return true;
        if (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(q)) return true;
        if (o.getPhone() != null && o.getPhone().toLowerCase().contains(q)) return true;
        if (o.getEmail() != null && o.getEmail().toLowerCase().contains(q)) return true;
        if (o.getCity() != null && o.getCity().toLowerCase().contains(q)) return true;
        if (o.getFulfillment() != null && o.getFulfillment().getAwbNumber() != null
                && o.getFulfillment().getAwbNumber().toLowerCase().contains(q)) return true;
        return false;
    }
}