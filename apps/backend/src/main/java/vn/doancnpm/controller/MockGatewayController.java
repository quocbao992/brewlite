package vn.doancnpm.controller;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/mock-gateway")
public class MockGatewayController {

    private static final Logger log = LoggerFactory.getLogger(MockGatewayController.class);

    @Value("${mock.gateway.delay-ms:500}")
    private long delayMs;
    @Value("${mock.gateway.success-rate:0.6}")
    private double successRate;
    @Value("${mock.gateway.base-url:http://localhost:8081}")
    private String baseUrl;
    @Value("${payment.gateway.secret:mock-secret}")
    private String secret;

    private final RestClient restClient = RestClient.create();

    @RequestMapping(value = "/pay", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<?> pay(
            @RequestParam Long orderId,
            @RequestParam(required = false) String amount,
            @RequestParam String method,
            @RequestParam String ref,
            @RequestParam(required = false) String result,
            @RequestHeader(value = "Accept", required = false) String acceptHeader) {

        String status = (result != null && !result.isBlank())
                ? result.trim().toUpperCase()
                : (ThreadLocalRandom.current().nextDouble() < successRate ? "SUCCESS" : "FAILED");
        String transactionId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "transactionId", transactionId,
                "status", status,
                "idempotencyKey", ref,
                "method", method);

        CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
                        .uri(baseUrl + "/api/v1/payments/notify")
                        .header("X-Gateway-Secret", secret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Mock gateway đã báo order {} = {}", orderId, status);
            } catch (Exception e) {
                log.error("Mock gateway báo về backend thất bại: {}", e.getMessage());
            }
        }, CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS));

        // Nếu mở từ trình duyệt (Accept text/html) thì trả về giao diện thanh toán HTML
        // trực quan
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);
            String badgeBg = isSuccess ? "#dcfce7" : "#fee2e2";
            String badgeColor = isSuccess ? "#15803d" : "#b91c1c";
            String badgeText = isSuccess ? "THANH TOÁN THÀNH CÔNG (MÔ PHỎNG)" : "THANH TOÁN THẤT BẠI (MÔ PHỎNG)";
            String statusColor = isSuccess ? "#16a34a" : "#dc2626";
            String noteText = isSuccess
                    ? "Hệ thống đã tự động gửi tín hiệu xác nhận thanh toán thành công về Backend!"
                    : "Cổng thanh toán đã gửi tín hiệu thanh toán thất bại (40% xác suất giả lập) về Backend!";

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">");
            sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            sb.append("<title>Cổng Thanh Toán Giả Lập BrewLite</title>");
            sb.append("<style>");
            sb.append(
                    "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }");
            sb.append(
                    ".card { background: #ffffff; padding: 36px 28px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.08); max-width: 440px; width: 100%; text-align: center; border: 1px solid #e2e8f0; }");
            sb.append(".badge { background: ").append(badgeBg).append("; color: ").append(badgeColor).append(
                    "; padding: 6px 14px; border-radius: 999px; font-weight: 700; font-size: 0.85rem; display: inline-block; margin-bottom: 16px; }");
            sb.append("h2 { margin: 0 0 12px; color: #1e293b; font-size: 1.4rem; }");
            sb.append(
                    ".info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f1f5f9; font-size: 0.92rem; color: #475569; }");
            sb.append(".info-row strong { color: #0f172a; }");
            sb.append(
                    ".btn { display: inline-block; background: #c17838; color: #ffffff; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: 700; margin-top: 24px; width: 100%; box-sizing: border-box; }");
            sb.append("</style></head><body><div class=\"card\">");
            sb.append("<div class=\"badge\">").append(badgeText).append("</div>");
            sb.append("<h2>Cổng Thanh Toán BrewLite</h2>");
            sb.append(
                    "<div style=\"background: #f8fafc; padding: 16px; border-radius: 10px; margin: 16px 0; text-align: left;\">");
            sb.append("<div class=\"info-row\"><span>Mã đơn:</span><strong>#BL").append(orderId)
                    .append("</strong></div>");
            sb.append("<div class=\"info-row\"><span>Cổng:</span><strong>").append(method).append("</strong></div>");
            sb.append("<div class=\"info-row\"><span>Mã giao dịch:</span><strong>").append(transactionId)
                    .append("</strong></div>");
            sb.append("<div class=\"info-row\"><span>Trạng thái:</span><strong style=\"color: ").append(statusColor)
                    .append(";\">").append(status).append("</strong></div>");
            sb.append("</div>");
            sb.append("<p style=\"color: #64748b; font-size: 0.88rem; margin: 0;\">").append(noteText).append("</p>");
            sb.append("<a href=\"http://localhost:3000\" class=\"btn\">Quay lại BrewLite Coffee</a>");
            sb.append("</div></body></html>");

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(sb.toString());
        }

        return ResponseEntity.accepted().body(Map.of(
                "message", "Đang xử lý thanh toán...",
                "orderId", orderId,
                "transactionId", transactionId,
                "simulatedResult", status,
                "willNotifyInMs", delayMs));
    }
}
