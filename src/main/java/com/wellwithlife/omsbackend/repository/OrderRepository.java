package com.wellwithlife.omsbackend.repository;

import com.wellwithlife.omsbackend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByReferenceCode(String referenceCode);

    // Admin queue: status-wise fetch
    List<Order> findByStatusOrderByCreatedAtDesc(String status);

    // BDE view: sirf apne banaye huye orders
    List<Order> findByCreatedBy_UserIdOrderByCreatedAtDesc(Long userId);

    // Reference screen stats count
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") String status);

    // KPI Counters
//    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.fulfillment f WHERE f.deliveryStatus = :deliveryStatus")
    long countByDeliveryStatus(@Param("deliveryStatus") String deliveryStatus);

    // Fail-safe Filter Query: checks both null and empty values
    @Query("SELECT o FROM Order o LEFT JOIN o.fulfillment f WHERE " +
            "(:status IS NULL OR :status = '' OR o.status = :status) AND " +
            "(:bdeId IS NULL OR o.createdBy.userId = :bdeId) AND " +
            "(:deliveryStatus IS NULL OR :deliveryStatus = '' OR f.deliveryStatus = :deliveryStatus) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(o.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " o.phone LIKE CONCAT('%', :search, '%'))")
    Page<Order> findOrdersWithFilters(
            @Param("status") String status,
            @Param("bdeId") Long bdeId,
            @Param("deliveryStatus") String deliveryStatus,
            @Param("search") String search,
            Pageable pageable
    );

    List<Order> findByStatus(String syncedToEasyecom);

    @Query("SELECT o FROM Order o LEFT JOIN o.fulfillment f WHERE o.status IN ('SYNCED_TO_EASYECOM', 'CONFIRMED') AND (f.awbNumber IS NULL OR f.awbNumber = '')")
    List<Order> findOrdersPendingEasyecomSync();
}