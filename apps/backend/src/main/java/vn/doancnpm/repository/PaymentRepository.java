package vn.doancnpm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.Payment;

@Repository 
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Payment findByTransactionRef(String transactionRef);
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderid);
    boolean existsByOrderIdAndStatus(Long orderId, String status);
} 