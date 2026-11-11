package vn.doancnpm.dto.Request;//

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToppingAddStockRequest {
    @NotNull(message = "toppingId không được để trống")
    private Long toppingId;

    @Positive(message = "Số lượng phải lớn hơn 0")
    private int amount;

    private String reason; // VD: "IMPORT", "DAMAGED", "ORDER_DEDUCT"
    
}