package vn.doancnpm.dto.Request;

import java.math.BigDecimal;//
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToppingRequest {
    
    @NotBlank(message = "Tên topping không được để trống")
    private String name;
 
    @NotNull(message = "Giá không được để trống")
    @Positive(message = "Giá phải lớn hơn 0")
    private BigDecimal unitPrice;

}