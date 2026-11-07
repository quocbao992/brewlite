package vn.doancnpm.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.doancnpm.domain.InventoryTopping;

@Repository
public interface InventoryToppingRepository extends JpaRepository<InventoryTopping, Long> {
    // Tìm tồn kho của 1 topping cụ thể theo ID topping
    Optional<InventoryTopping> findByToppingId(Long toppingId);
}