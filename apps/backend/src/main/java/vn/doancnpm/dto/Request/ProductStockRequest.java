package vn.doancnpm.dto.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStockRequest {
    @NotNull(message = "productId không được để trống")
    private Long productId;

    @Positive(message = "Số lượng phải lớn hơn 0")
    private int amount;

    private String reason; // VD: "IMPORT", "DAMAGED", "ORDER_DEDUCT"
}