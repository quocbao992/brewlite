package vn.doancnpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.OrderItem;
import java.util.List; 

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderCreatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
    List<OrderItem> findByOrderId(Long orderId);
}
