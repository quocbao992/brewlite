package vn.doancnpm.dto.Response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemToppingResponse {
  private Long id;
  private String name;
  private Long toppingId;
  private BigDecimal unitPrice;
  private int quantity;
  private BigDecimal subtotal;
}
