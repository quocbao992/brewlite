package vn.doancnpm.service.implement;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.doancnpm.domain.Cart;
import vn.doancnpm.domain.CartItem;
import vn.doancnpm.domain.CartItemTopping;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.dto.Request.CartItemToppingRequest;
import vn.doancnpm.dto.Request.CartRequest;
import vn.doancnpm.dto.Response.CartItemResponse;
import vn.doancnpm.dto.Response.CartItemToppingResponse;
import vn.doancnpm.dto.Response.CartResponse;
import vn.doancnpm.repository.CartItemRepository;
import vn.doancnpm.repository.CartRepository;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.service.CartService;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
@Transactional
public class ICartService implements CartService {

  private final CartItemRepository cartItemRepo;
  private final CartRepository cartRepo;
  private final CustomerRepository customerRepo;
  private final ProductRepository productRepo;
  private final ToppingRepository toppingRepo;

  @Override
  public CartResponse addToCart(CartRequest rq) {
    Customer customer =
        customerRepo
            .findById(rq.getCustomerId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

    Cart cart =
        cartRepo
            .findByCustomerId(customer.getId())
            .orElseGet(() -> cartRepo.save(Cart.builder().customer(customer).build()));

    Product product =
        productRepo
            .findById(rq.getItem().getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

    CartItem incomingItem = maptoCartItem(rq);
    incomingItem.setCart(cart);
    incomingItem.setProduct(product);
    incomingItem.setUnitPrice(product.getUnitPrice());

    List<CartItem> existingItems =
        cartItemRepo.findByCartIdAndProductId(cart.getId(), product.getId());

    CartItem matchedItem = null;
    for (CartItem item : existingItems) {
      boolean sameSize = Objects.equals(item.getSize(), incomingItem.getSize());
      if (sameSize && sameTopping(item.getToppings(), incomingItem.getToppings())) {
        matchedItem = item;
        break;
      }
    }

    if (matchedItem != null) {
      matchedItem.setQuantity(matchedItem.getQuantity() + incomingItem.getQuantity());
      cartItemRepo.save(matchedItem);
    } else {
      cartItemRepo.save(incomingItem);
    }

    return mapToCartResponse(cart);
  }

  @Override
  public CartResponse updateCartItem(Long cartItemId, int quantity) {
    if (quantity < 1) {
      throw new IllegalArgumentException("Số lượng món phải lớn hơn hoặc bằng 1");
    }
    CartItem cartItem =
        cartItemRepo
            .findById(cartItemId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món trong giỏ hàng"));

    cartItem.setQuantity(quantity);
    cartItemRepo.save(cartItem);

    return mapToCartResponse(cartItem.getCart());
  }

  @Override
  public void removeCartItem(Long cartItemId) {
    CartItem cartItem =
        cartItemRepo
            .findById(cartItemId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món trong giỏ hàng"));
    cartItemRepo.delete(cartItem);
  }

  @Override
  public void clearCart(Long customerId) {
    Customer customer =
        customerRepo
            .findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

    Cart cart =
        cartRepo
            .findByCustomerId(customer.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy giỏ hàng của khách hàng"));

    List<CartItem> items = cartItemRepo.findByCart(cart);
    if (!items.isEmpty()) {
      cartItemRepo.deleteAll(items);
    }
  }

  @Override
  public CartResponse getCart(Long customerId) {
    Customer customer =
        customerRepo
            .findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

    Cart cart =
        cartRepo
            .findByCustomerId(customer.getId())
            .orElseGet(() -> cartRepo.save(Cart.builder().customer(customer).build()));

    return mapToCartResponse(cart);
  }

  public boolean sameTopping(List<CartItemTopping> list1, List<CartItemTopping> list2) {
    boolean empty1 = (list1 == null || list1.isEmpty());
    boolean empty2 = (list2 == null || list2.isEmpty());

    if (empty1 && empty2) return true;
    if (empty1 || empty2) return false;
    if (list1.size() != list2.size()) return false;

    for (CartItemTopping t1 : list1) {
      boolean matched =
          list2.stream()
              .anyMatch(
                  t2 ->
                      Objects.equals(t2.getTopping().getId(), t1.getTopping().getId())
                          && t2.getQuantity() == t1.getQuantity());
      if (!matched) return false;
    }

    return true;
  }

  private CartItem maptoCartItem(CartRequest rq) {
    List<CartItemToppingRequest> toppingRequests = rq.getItem().getToppings();
    Map<Long, Topping> toppingMap = Collections.emptyMap();

    if (toppingRequests != null && !toppingRequests.isEmpty()) {
      List<Long> toppingIds =
          toppingRequests.stream().map(CartItemToppingRequest::getToppingId).toList();
      toppingMap =
          toppingRepo.findAllById(toppingIds).stream()
              .collect(Collectors.toMap(Topping::getId, t -> t));
    }

    CartItem cartItem =
        CartItem.builder()
            .quantity(rq.getItem().getQuantity())
            .size(rq.getItem().getSize()) // Đã map size
            .note(rq.getItem().getNote()) // Đã map note
            .build();

    List<CartItemTopping> itemToppings = new java.util.ArrayList<>();

    if (toppingRequests != null) {
      for (CartItemToppingRequest topRq : toppingRequests) {
        Topping t = toppingMap.get(topRq.getToppingId());
        if (t == null) {
          throw new IllegalArgumentException("Không tìm thấy topping id: " + topRq.getToppingId());
        }

        CartItemTopping itemTopping =
            CartItemTopping.builder()
                .cartItem(cartItem)
                .topping(t)
                .price(t.getUnitPrice())
                .quantity(topRq.getQuantity())
                .build();

        itemToppings.add(itemTopping);
      }
    }

    cartItem.setToppings(itemToppings);

    return cartItem;
  }

  private CartItemToppingResponse mapToToppingResponse(CartItemTopping cit) {
    BigDecimal subtotal =
        (cit.getPrice() != null)
            ? cit.getPrice().multiply(BigDecimal.valueOf(cit.getQuantity()))
            : BigDecimal.ZERO;

    return CartItemToppingResponse.builder()
        .id(cit.getId())
        .toppingId(cit.getTopping().getId())
        .name(cit.getTopping().getName())
        .unitPrice(cit.getPrice())
        .quantity(cit.getQuantity())
        .subtotal(subtotal)
        .build();
  }

  private CartItemResponse mapToCartItemResponse(CartItem item) {
    BigDecimal toppingTotal = BigDecimal.ZERO;
    List<CartItemToppingResponse> toppingResponses = Collections.emptyList();

    if (item.getToppings() != null && !item.getToppings().isEmpty()) {
      toppingTotal =
          item.getToppings().stream()
              .map(
                  cit ->
                      cit.getPrice()
                          .multiply(BigDecimal.valueOf(cit.getQuantity() * item.getQuantity())))
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      toppingResponses = item.getToppings().stream().map(this::mapToToppingResponse).toList();
    }

    BigDecimal basePrice =
        (item.getProduct().getUnitPrice() != null)
            ? item.getProduct().getUnitPrice()
            : BigDecimal.ZERO;
    BigDecimal singleItemPrice = basePrice.add(toppingTotal);

    BigDecimal itemTotalPrice = singleItemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

    return CartItemResponse.builder()
        .id(item.getId())
        .productId(item.getProduct().getId())
        .name(item.getProduct().getName())
        .unitPrice(item.getProduct().getUnitPrice())
        .quantity(item.getQuantity())
        .size(item.getSize())
        .note(item.getNote())
        .toppings(toppingResponses)
        .img(item.getProduct().getImgURL())
        .itemTotalPrice(itemTotalPrice)
        .build();
  }

  private CartResponse mapToCartResponse(Cart cart) {
    List<CartItem> items = cartItemRepo.findByCart(cart);

    List<CartItemResponse> itemResponses = items.stream().map(this::mapToCartItemResponse).toList();

    int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();

    BigDecimal totalPrice =
        itemResponses.stream()
            .map(CartItemResponse::getItemTotalPrice)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return CartResponse.builder()
        .id(cart.getId())
        .customerId(cart.getCustomer() != null ? cart.getCustomer().getId() : null)
        .items(itemResponses)
        .totalItems(totalItems)
        .totalPrice(totalPrice)
        .build();
  }
}
