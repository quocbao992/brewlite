package vn.doancnpm.dto.Response;

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
public class CartItemResponse {
  private Long id;
  private Long productId;
  private String name;
  private String img;
  private int quantity;
  private Size size;
  private BigDecimal unitPrice;
  private String note;
  private List<CartItemToppingResponse> toppings;
  private BigDecimal itemTotalPrice;
}
