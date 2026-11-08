package vn.doancnpm.dto.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryActionRequest {
    /*
    private String type;   // "PRODUCT" hoặc "TOPPING"
    private Long id;       // productId hoặc toppingId
    private Integer amount;
    private String reason; */
    @NotNull(message = "type không được để trống")
    @Pattern(regexp = "PRODUCT|TOPPING", message = "type chỉ nhận PRODUCT hoặc TOPPING")
    private String type;   // "PRODUCT" hoặc "TOPPING"
 
    @NotNull(message = "id không được để trống")
    private Long id;       // productId hoặc toppingId
 
    @NotNull(message = "amount không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer amount;

    private String reason;
}

