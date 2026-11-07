package vn.doancnpm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.ProductLog;

@Repository
public interface ProductLogRepository extends JpaRepository<ProductLog, Long> {
    List<ProductLog> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<ProductLog> findAllByOrderByCreatedAtDesc();
}
