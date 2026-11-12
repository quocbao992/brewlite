package vn.doancnpm.dto.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.doancnpm.domain.CartItem.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {
  @NotNull(message = "productID không được để trống")
  private Long productId;

  @Min(value = 1, message = " số lượng phải lớn hơn 0")
  private int quantity;

  @NotNull(message = "size không được để trống ")
  private Size size;

  private Long customerId;

  private List<CartItemToppingRequest> toppings;

  private String note;

  private BigDecimal unitPrice;
}
