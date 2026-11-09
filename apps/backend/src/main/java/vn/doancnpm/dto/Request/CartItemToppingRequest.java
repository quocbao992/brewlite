package vn.doancnpm.dto.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CartItemToppingRequest {
  @NotNull(message = " toppingID không được để trống")
  private Long toppingId;

  @Min(value = 1, message = "quantity phải lớn hơn 0")
  private int quantity;
}
