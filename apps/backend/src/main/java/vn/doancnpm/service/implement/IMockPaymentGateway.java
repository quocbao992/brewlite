package vn.doancnpm.service.implement;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import vn.doancnpm.service.PaymentGatewayService;

@Component
public class IMockPaymentGateway implements PaymentGatewayService {

    @Value("${mock.gateway.base-url:http://localhost:8081}")
    private String baseUrl;

    // Cổng thật sẽ trả link VNPay/Momo. Mock trả link tới endpoint giả lập bên dưới.
    @Override
    public String createPaymentUrl(Long orderId, BigDecimal amount, String method, String idempotencyKey) {
        return baseUrl + "/mock-gateway/pay?orderId=" + orderId
                + "&amount=" + amount.toPlainString()
                + "&method=" + URLEncoder.encode(method, StandardCharsets.UTF_8)
                + "&ref=" + URLEncoder.encode(idempotencyKey, StandardCharsets.UTF_8);
        }
}