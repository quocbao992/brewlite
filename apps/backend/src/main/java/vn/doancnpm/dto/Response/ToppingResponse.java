package vn.doancnpm.dto.Response;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToppingResponse {
    private Long id;
    private String name;
    private BigDecimal unitPrice;
    private Integer stock; // Hiển thị luôn tồn kho lấy từ bảng InventoryTopping
    private Boolean status;
}