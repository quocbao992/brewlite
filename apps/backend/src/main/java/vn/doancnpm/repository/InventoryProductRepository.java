package vn.doancnpm.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.doancnpm.domain.InventoryProduct;

@Repository
public interface InventoryProductRepository extends JpaRepository<InventoryProduct, Long> {
    Optional<InventoryProduct> findByProductId(Long productId);
}