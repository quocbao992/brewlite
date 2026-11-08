package vn.doancnpm.dto.Request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {
    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String code;

    private String description;

    @NotBlank(message = "Loại giảm giá không được để trống (PERCENT hoặc FIXED)")
    private String discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    private BigDecimal discountValue;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer usageLimit;

    private Boolean active;
}

