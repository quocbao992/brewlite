package vn.doancnpm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.doancnpm.dto.Request.CartRequest;
import vn.doancnpm.service.CartService;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;

  @GetMapping
  public ResponseEntity<?> getCart(@RequestParam Long customerId) {
    return ResponseEntity.ok(cartService.getCart(customerId));
  }

  @PostMapping("/items")
  public ResponseEntity<String> addToCart(@RequestBody CartRequest rq) {
    cartService.addToCart(rq);
    return new ResponseEntity<>("Thêm vào giỏ hàng thành công !", HttpStatus.CREATED);
  }

  public record UQRequest(int quantity) {}

  @PutMapping("/items/{cartItemId}")
  public ResponseEntity<String> updateCartItem(
      @PathVariable Long cartItemId, @RequestBody UQRequest rq) {
    cartService.updateCartItem(cartItemId, rq.quantity);
    return new ResponseEntity<>("Cập nhật số lượng thành công !", HttpStatus.OK);
  }

  @DeleteMapping("/items/{cartItemId}")
  public ResponseEntity<String> removeCartItem(@PathVariable Long cartItemId) {
    cartService.removeCartItem(cartItemId);
    return new ResponseEntity<>("Xóa món khỏi giỏ hàng thành công !", HttpStatus.OK);
  }

  @DeleteMapping
  public ResponseEntity<String> clearCart(@RequestParam Long customerId) {
    cartService.clearCart(customerId);
    return new ResponseEntity<>("Đã làm trống giỏ hàng !", HttpStatus.OK);
  }
}
