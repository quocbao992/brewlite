package vn.doancnpm.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.Topping;
@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {
    List<Topping> findByStatus(boolean status);
    Optional<Topping> findById(Long id);
    
}
