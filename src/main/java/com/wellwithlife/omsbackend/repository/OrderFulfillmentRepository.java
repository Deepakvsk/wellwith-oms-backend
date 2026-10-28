package com.wellwithlife.omsbackend.repository;

import com.wellwithlife.omsbackend.entity.OrderFulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderFulfillmentRepository extends JpaRepository<OrderFulfillment, Long> {
    Optional<OrderFulfillment> findByOrder_OrderId(Long orderId);
    Optional<OrderFulfillment> findByEasyecomOrderId(String easyecomOrderId);
}