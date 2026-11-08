package vn.doancnpm.dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.doancnpm.domain.Order;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long customerId;
    private Order.Status status;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private int loyaltyPointsUsed;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String orderCode;
    private Order.OrderType orderType;
    private String tableNumber;
    private String note;
}
