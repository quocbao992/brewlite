package vn.doancnpm.dto.Request;

import vn.doancnpm.domain.Payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyRequest {
    @NotNull
    private Long orderId;

    private String transactionId;

    @NotBlank
    private String status;   // SUCCESS hoặc FAILED

    @NotBlank
    private String idempotencyKey;

    @NotNull
    private Payment.Method method;
}