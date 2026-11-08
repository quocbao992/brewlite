package vn.doancnpm.dto.Response;

import java.math.BigDecimal;
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

public class OrderItemToppingResponse {
    private Long toppingId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;

}


