package vn.doancnpm.dto.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDeductStockRequest {
    @NotNull(message = "productId không được để trống")
    private Long productId;
    @NotNull(message = "orderId không được để trống")
    private Long orderId;
    @Positive(message = "Số lượng phải lớn hơn 0")
    private int amount;

}