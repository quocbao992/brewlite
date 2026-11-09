package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.doancnpm.domain.Cart;
import vn.doancnpm.domain.CartItem;
import vn.doancnpm.domain.CartItemTopping;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.CartItemRequest;
import vn.doancnpm.dto.Request.CartItemToppingRequest;
import vn.doancnpm.dto.Request.CartRequest;
import vn.doancnpm.dto.Response.CartResponse;
import vn.doancnpm.repository.CartItemRepository;
import vn.doancnpm.repository.CartRepository;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.service.implement.ICartService;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

  @Mock
  private CustomerRepository customerRepo;

  @Mock
  private CartItemRepository cartItemRepo;

  @Mock
  private CartRepository cartRepo;

  @Mock
  private ProductRepository productRepo;

  @Mock
  private ToppingRepository toppingRepo;

  @InjectMocks
  private ICartService cartService;

  private User sampleUser;
  private Customer customer;
  private Cart cart;
  private Product product;
  private Topping topping;

  @BeforeEach
  void setUp() {

    sampleUser = new User();
    sampleUser.setId(10L);
    sampleUser.setUsername("customer_user");
    sampleUser.setFullName("Nguyen Van A");
    sampleUser.setEmail("nguyenvana@example.com");
    sampleUser.setPasswordHash("hashed_secret");
    sampleUser.setRole("ROLE_CUSTOMER");
    sampleUser.setStatus(true);

    customer = Customer.builder()
        .id(1L)
        .user(sampleUser)
        .phone("0912345678")
        .gender(true)
        .loyaltyP(0.0f)
        .build();

    cart = Cart.builder().id(10L).customer(customer).build();

    product = Product.builder()
        .id(100L)
        .name("Trà sữa")
        .unitPrice(BigDecimal.valueOf(30000))
        .imgURL("https://example.com/trasua.png")
        .build();

    topping = Topping.builder()
        .id(200L)
        .name("Trân châu đen")
        .unitPrice(BigDecimal.valueOf(5000))
        .build();
  }

  // ==========================================
  // 1. addToCart Tests
  // ==========================================
  @Nested
  @DisplayName("addToCart tests")
  class AddToCartTests {
    @Test
    @DisplayName("Ném exception khi không tìm thấy Topping trong DB")
    void addToCart_ThrowsException_WhenToppingNotFound() {
      // Gửi request kèm topping ID 999L không tồn tại
      CartItemToppingRequest topRq = CartItemToppingRequest.builder().toppingId(999L).quantity(1).build();
      CartItemRequest itemRq = CartItemRequest.builder()
          .productId(100L)
          .quantity(1)
          .size(CartItem.Size.M)
          .toppings(List.of(topRq))
          .build();
      CartRequest request = CartRequest.builder().customerId(1L).item(itemRq).build();

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(productRepo.findById(100L)).thenReturn(Optional.of(product));
      // Mock trả về danh sách rỗng khi tìm topping ID 999L
      when(toppingRepo.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

      assertThatThrownBy(() -> cartService.addToCart(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Không tìm thấy topping id: 999");

      verify(cartItemRepo, never()).save(any());
    }

    @Test
    @DisplayName("Không gộp số lượng khi cùng sản phẩm nhưng khác size")
    void addToCart_AddNewItem_WhenSameProductDifferentSize() {
      // Đã có sẵn món size M trong giỏ
      CartItem existingItem = CartItem.builder()
          .id(50L)
          .cart(cart)
          .product(product)
          .size(CartItem.Size.M)
          .quantity(1)
          .toppings(Collections.emptyList())
          .build();

      // Khách thêm cùng sản phẩm đó nhưng chọn size L
      CartItemRequest itemRq = CartItemRequest.builder()
          .productId(100L)
          .quantity(1)
          .size(CartItem.Size.L)
          .toppings(Collections.emptyList())
          .build();
      CartRequest request = CartRequest.builder().customerId(1L).item(itemRq).build();

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(productRepo.findById(100L)).thenReturn(Optional.of(product));
      when(cartItemRepo.findByCartIdAndProductId(10L, 100L)).thenReturn(List.of(existingItem));
      when(cartItemRepo.findByCart(cart)).thenReturn(List.of(existingItem));

      cartService.addToCart(request);

      // Kiểm tra: existingItem KHÔNG được tăng số lượng
      assertThat(existingItem.getQuantity()).isEqualTo(1);

      // Kiểm tra: Phải gọi save một instance CartItem mới với size L
      ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
      verify(cartItemRepo).save(captor.capture());
      assertThat(captor.getValue().getSize()).isEqualTo(CartItem.Size.L);
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy khách hàng")
    void addToCart_ThrowsException_WhenCustomerNotFound() {
      CartRequest request = CartRequest.builder().customerId(999L).build();

      when(customerRepo.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cartService.addToCart(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Không tìm thấy khách hàng");
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void addToCart_ThrowsException_WhenProductNotFound() {
      CartItemRequest itemRq = CartItemRequest.builder().productId(999L).build();
      CartRequest request = CartRequest.builder().customerId(1L).item(itemRq).build();

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(productRepo.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cartService.addToCart(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Không tìm thấy sản phẩm");
    }

    @Test
    @DisplayName("Thêm món mới khi giỏ hàng chưa có sản phẩm tương tự")
    void addToCart_AddNewItem_WhenNoMatchingItemExists() {
      CartItemRequest itemRq = CartItemRequest.builder()
          .productId(100L)
          .quantity(2)
          .size(CartItem.Size.M)
          .note("Ít ngọt")
          .toppings(Collections.emptyList())
          .build();
      CartRequest request = CartRequest.builder().customerId(1L).item(itemRq).build();

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(productRepo.findById(100L)).thenReturn(Optional.of(product));
      when(cartItemRepo.findByCartIdAndProductId(10L, 100L)).thenReturn(Collections.emptyList());

      CartItem savedItem = CartItem.builder()
          .id(1L)
          .cart(cart)
          .product(product)
          .quantity(2)
          .size(CartItem.Size.M)
          .build();
      when(cartItemRepo.findByCart(cart)).thenReturn(List.of(savedItem));

      CartResponse response = cartService.addToCart(request);

      assertThat(response).isNotNull();
      assertThat(response.getTotalItems()).isEqualTo(2);

      ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
      verify(cartItemRepo, times(1)).save(captor.capture());
      CartItem captured = captor.getValue();
      assertThat(captured.getSize()).isEqualTo(CartItem.Size.M);
      assertThat(captured.getQuantity()).isEqualTo(2);
      assertThat(captured.getUnitPrice()).isEqualTo(BigDecimal.valueOf(30000));
    }

    @Test
    @DisplayName("Gộp số lượng khi sản phẩm cùng size và topping đã có sẵn trong giỏ")
    void addToCart_MergeQuantity_WhenMatchingItemAlreadyExists() {
      CartItemToppingRequest topRq = CartItemToppingRequest.builder().toppingId(200L).quantity(1).build();
      CartItemRequest itemRq = CartItemRequest.builder()
          .productId(100L)
          .quantity(1)
          .size(CartItem.Size.L)
          .toppings(List.of(topRq))
          .build();
      CartRequest request = CartRequest.builder().customerId(1L).item(itemRq).build();

      CartItem existingItem = CartItem.builder()
          .id(50L)
          .cart(cart)
          .product(product)
          .size(CartItem.Size.L)
          .quantity(2)
          .build();
      CartItemTopping existingTopping = CartItemTopping.builder()
          .topping(topping)
          .quantity(1)
          .price(topping.getUnitPrice())
          .build();
      existingItem.setToppings(List.of(existingTopping));

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(productRepo.findById(100L)).thenReturn(Optional.of(product));
      when(toppingRepo.findAllById(List.of(200L))).thenReturn(List.of(topping));
      when(cartItemRepo.findByCartIdAndProductId(10L, 100L)).thenReturn(List.of(existingItem));
      when(cartItemRepo.findByCart(cart)).thenReturn(List.of(existingItem));

      cartService.addToCart(request);

      verify(cartItemRepo).save(existingItem);
      assertThat(existingItem.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Tự tạo mới Cart nếu khách hàng chưa có giỏ")
    void addToCart_CreateCartIfNotExist() {
      CartItemRequest itemRq = CartItemRequest.builder()
          .productId(100L)
          .quantity(1)
          .size(CartItem.Size.M)
          .toppings(Collections.emptyList())
          .build();
      CartRequest request = CartRequest.builder().customerId(1L).item(itemRq).build();

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.empty());
      when(cartRepo.save(any(Cart.class))).thenReturn(cart);
      when(productRepo.findById(100L)).thenReturn(Optional.of(product));
      when(cartItemRepo.findByCartIdAndProductId(cart.getId(), product.getId()))
          .thenReturn(Collections.emptyList());
      when(cartItemRepo.findByCart(cart)).thenReturn(Collections.emptyList());

      cartService.addToCart(request);

      verify(cartRepo, times(1)).save(any(Cart.class));
    }
  }

  // ==========================================
  // 2. updateCartItem Tests
  // ==========================================
  @Nested
  @DisplayName("updateCartItem tests")
  class UpdateCartItemTests {

    @Test
    @DisplayName("Ném exception khi quantity < 1")
    void updateCartItem_ThrowsException_WhenQuantityLessThanOne() {
      assertThatThrownBy(() -> cartService.updateCartItem(1L, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số lượng món phải lớn hơn hoặc bằng 1");

      verify(cartItemRepo, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy CartItem")
    void updateCartItem_ThrowsException_WhenItemNotFound() {
      when(cartItemRepo.findById(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cartService.updateCartItem(1L, 3))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Không tìm thấy món trong giỏ hàng");
    }

    @Test
    @DisplayName("Cập nhật số lượng món thành công")
    void updateCartItem_Success() {
      CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();

      when(cartItemRepo.findById(1L)).thenReturn(Optional.of(item));
      when(cartItemRepo.findByCart(cart)).thenReturn(List.of(item));

      CartResponse response = cartService.updateCartItem(1L, 5);

      assertThat(item.getQuantity()).isEqualTo(5);
      verify(cartItemRepo).save(item);
      assertThat(response).isNotNull();
    }
  }

  // ==========================================
  // 3. removeCartItem Tests
  // ==========================================
  @Nested
  @DisplayName("removeCartItem tests")
  class RemoveCartItemTests {

    @Test
    @DisplayName("Xóa món thành công khỏi giỏ hàng")
    void removeCartItem_Success() {
      CartItem item = CartItem.builder().id(1L).cart(cart).build();
      when(cartItemRepo.findById(1L)).thenReturn(Optional.of(item));

      cartService.removeCartItem(1L);

      verify(cartItemRepo, times(1)).delete(item);
    }

    @Test
    @DisplayName("Ném exception khi xóa món không tồn tại")
    void removeCartItem_NotFound() {
      when(cartItemRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cartService.removeCartItem(99L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Không tìm thấy món trong giỏ hàng");
    }
  }

  // ==========================================
  // 4. clearCart Tests
  // ==========================================
  @Nested
  @DisplayName("clearCart tests")
  class ClearCartTests {

    @Test
    @DisplayName("Ném exception khi clearCart của khách hàng chưa có giỏ")
    void clearCart_ThrowsException_WhenCartNotFound() {
      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cartService.clearCart(1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Không tìm thấy giỏ hàng của khách hàng");

      verify(cartItemRepo, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Xóa tất cả món trong giỏ hàng thành công")
    void clearCart_Success() {
      List<CartItem> items = List.of(CartItem.builder().id(1L).build(), CartItem.builder().id(2L).build());

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(cartItemRepo.findByCart(cart)).thenReturn(items);

      cartService.clearCart(1L);

      verify(cartItemRepo, times(1)).deleteAll(items);
    }

    @Test
    @DisplayName("Không gọi deleteAll khi giỏ hàng rỗng")
    void clearCart_EmptyCart_DoesNotCallDeleteAll() {
      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(cartItemRepo.findByCart(cart)).thenReturn(Collections.emptyList());

      cartService.clearCart(1L);

      verify(cartItemRepo, never()).deleteAll(any());
    }
  }

  // ==========================================
  // 5. getCart Tests
  // ==========================================
  @Nested
  @DisplayName("getCart tests")
  class GetCartTests {

    @Test
    @DisplayName("Tính toán chính xác tổng tiền của giỏ hàng gồm base price và topping")
    void getCart_CalculatesTotalPriceCorrectly() {
      // Giả lập 1 món: Giá gốc 30.000, 1 topping giá 5.000, số lượng 2 ly
      // singleItemPrice = 30.000 + (5.000 * 1 * 2) = 40.000
      // itemTotalPrice = singleItemPrice * 2 = 80.000
      CartItemTopping itemTopping = CartItemTopping.builder()
          .id(1L)
          .topping(topping)
          .price(topping.getUnitPrice())
          .quantity(1)
          .build();

      CartItem cartItem = CartItem.builder()
          .id(10L)
          .cart(cart)
          .product(product)
          .size(CartItem.Size.M)
          .quantity(2)
          .toppings(List.of(itemTopping))
          .build();

      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(cartItemRepo.findByCart(cart)).thenReturn(List.of(cartItem));

      CartResponse response = cartService.getCart(1L);

      // singleItemPrice = 30,000 + (5,000 * 1 * 2) = 40,000
      // itemTotalPrice = 40,000 * 2 = 80,000
      BigDecimal expectedTotalPrice = new BigDecimal("80000");

      assertThat(response).isNotNull();
      assertThat(response.getTotalItems()).isEqualTo(2);
      assertThat(response.getItems().get(0).getItemTotalPrice())
          .isEqualByComparingTo(expectedTotalPrice);
      assertThat(response.getTotalPrice()).isEqualByComparingTo(expectedTotalPrice);
    }

    @Test
    @DisplayName("Lấy giỏ hàng thành công khi khách hàng đã có giỏ")
    void getCart_Success_ExistingCart() {
      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
      when(cartItemRepo.findByCart(cart)).thenReturn(Collections.emptyList());

      CartResponse response = cartService.getCart(1L);

      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(10L);
      assertThat(response.getCustomerId()).isEqualTo(1L);
      verify(cartRepo, never()).save(any());
    }

    @Test
    @DisplayName("Tự động tạo giỏ hàng mới nếu khách hàng chưa có")
    void getCart_CreatesCart_WhenNotExist() {
      when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
      when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.empty());
      when(cartRepo.save(any(Cart.class))).thenReturn(cart);
      when(cartItemRepo.findByCart(cart)).thenReturn(Collections.emptyList());

      CartResponse response = cartService.getCart(1L);

      assertThat(response).isNotNull();
      verify(cartRepo, times(1)).save(any(Cart.class));
    }
  }

  // ==========================================
  // 6. sameTopping Tests (Helper Method)
  // ==========================================
  @Nested
  @DisplayName("sameTopping helper method tests")
  class SameToppingTests {

    @Test
    @DisplayName("Hai danh sách rỗng hoặc null phải trả về true")
    void sameTopping_BothEmptyOrNull_ReturnsTrue() {
      assertThat(cartService.sameTopping(null, null)).isTrue();
      assertThat(cartService.sameTopping(new ArrayList<>(), Collections.emptyList())).isTrue();
      assertThat(cartService.sameTopping(null, Collections.emptyList())).isTrue();
    }

    @Test
    @DisplayName("Một danh sách rỗng và một có topping phải trả về false")
    void sameTopping_OneEmptyOneNot_ReturnsFalse() {
      CartItemTopping t1 = CartItemTopping.builder().topping(topping).quantity(1).build();
      assertThat(cartService.sameTopping(List.of(t1), null)).isFalse();
      assertThat(cartService.sameTopping(Collections.emptyList(), List.of(t1))).isFalse();
    }

    @Test
    @DisplayName("Cùng topping và cùng số lượng phải trả về true")
    void sameTopping_SameIdAndQuantity_ReturnsTrue() {
      CartItemTopping t1 = CartItemTopping.builder().topping(topping).quantity(2).build();
      CartItemTopping t2 = CartItemTopping.builder().topping(topping).quantity(2).build();

      assertThat(cartService.sameTopping(List.of(t1), List.of(t2))).isTrue();
    }

    @Test
    @DisplayName("Cùng topping nhưng khác số lượng phải trả về false")
    void sameTopping_SameIdDifferentQuantity_ReturnsFalse() {
      CartItemTopping t1 = CartItemTopping.builder().topping(topping).quantity(1).build();
      CartItemTopping t2 = CartItemTopping.builder().topping(topping).quantity(2).build();

      assertThat(cartService.sameTopping(List.of(t1), List.of(t2))).isFalse();
    }
  }
}
