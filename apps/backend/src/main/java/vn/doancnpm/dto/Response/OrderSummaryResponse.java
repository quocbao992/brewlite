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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private Long id;
    private String orderCode;
    private Order.Status status;
    private Order.OrderType orderType;
    private String tableNumber;
    private BigDecimal totalAmount;
    private int totalItems;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
}