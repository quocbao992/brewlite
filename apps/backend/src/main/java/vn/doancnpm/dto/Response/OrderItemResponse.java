package vn.doancnpm.dto.Response;
import java.math.BigDecimal;
import java.util.List;
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

public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private String size;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private List<OrderItemToppingResponse> toppings;
}


