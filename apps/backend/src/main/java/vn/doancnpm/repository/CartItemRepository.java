package vn.doancnpm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.Cart;
import vn.doancnpm.domain.CartItem;

@Repository 
public interface CartItemRepository extends JpaRepository <CartItem, Long> {
 
@Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
List<CartItem> findByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

@Query("SELECT ci FROM CartItem ci WHERE ci.cart = :cart")
List<CartItem> findByCart(@Param("cart") Cart cart);
    
} 