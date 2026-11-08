package vn.doancnpm.dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class AdminOrderResponse {
    private Long id;
    private String orderCode;
    private String customerName;
    private String phone;
    private BigDecimal totalAmount;
    private Order.Status status;
    private LocalDateTime createdAt;
}