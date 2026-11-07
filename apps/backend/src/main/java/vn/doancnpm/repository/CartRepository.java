package vn.doancnpm.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.Cart;

@Repository 
public interface CartRepository extends JpaRepository <Cart, Long> {
    Optional<Cart> findByCustomerId(Long id);
    
    
}
