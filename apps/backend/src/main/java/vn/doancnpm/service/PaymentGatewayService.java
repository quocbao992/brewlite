package vn.doancnpm.service;

import java.math.BigDecimal;

public interface PaymentGatewayService {
    String createPaymentUrl(Long orderId, BigDecimal amount, String method, String idempotencyKey);
}