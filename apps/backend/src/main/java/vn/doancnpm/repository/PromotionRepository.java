package vn.doancnpm.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.doancnpm.domain.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);
    boolean existsByCode(String code);
}
