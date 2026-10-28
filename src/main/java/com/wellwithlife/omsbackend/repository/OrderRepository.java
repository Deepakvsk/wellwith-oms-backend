package com.wellwithlife.omsbackend.repository;

import com.wellwithlife.omsbackend.entity.Order;
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
}