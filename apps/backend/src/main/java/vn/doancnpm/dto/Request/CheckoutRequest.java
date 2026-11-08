package vn.doancnpm.dto.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.doancnpm.domain.Order;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotNull
    private Long customerId; // TẠM: sau khi có JWT thì lấy từ token, bỏ field này

    @NotNull
    private Order.OrderType orderType;

    private String tableNumber;

    private String voucherCode;

    @Min(0)
    private Integer loyaltyPointsUsed;

    @Size(max = 500)
    private String note;

    //@NotNull
    //private Order.PaymentMethod paymentMethod;
}