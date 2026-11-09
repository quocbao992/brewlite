package vn.doancnpm.service;

import vn.doancnpm.dto.Request.CartRequest;
import vn.doancnpm.dto.Response.CartResponse;

public interface CartService {
  CartResponse getCart(Long customerId);

  CartResponse addToCart(CartRequest rq);

  CartResponse updateCartItem(Long cartItemId, int quantity);

  void removeCartItem(Long cartItemId);

  void clearCart(Long customerId);
}
