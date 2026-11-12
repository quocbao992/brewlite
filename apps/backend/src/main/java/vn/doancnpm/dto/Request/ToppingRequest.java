package vn.doancnpm.dto.Request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToppingRequest {
    
    @NotBlank(message = "Tên topping không được để trống")
    @Size(max = 100, message = "Tên topping không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N} ]*$", message = "Tên topping không được chứa ký tự đặc biệt"    )
    private String name;
 
    @NotNull(message = "Giá không được để trống")
    @Positive(message = "Giá phải lớn hơn 0")
    private BigDecimal unitPrice;

}