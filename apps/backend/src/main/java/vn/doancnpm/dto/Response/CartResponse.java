package vn.doancnpm.dto.Response;

import java.math.BigDecimal;
import java.util.List;
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
public class CartResponse {
  private Long id;
  private Long customerId;
  private List<CartItemResponse> items;
  private int totalItems;
  private BigDecimal totalPrice;
}
