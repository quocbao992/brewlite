package vn.doancnpm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.doancnpm.dto.Request.PaymentNotifyRequest;
import vn.doancnpm.dto.Request.PaymentRequest;
import vn.doancnpm.dto.Response.PaymentResponse;
import vn.doancnpm.service.PaymentService;
import vn.doancnpm.controller.PaymentController;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock PaymentService paymentService;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentService);
        ReflectionTestUtils.setField(controller, "gatewaySecret", "mock-secret");
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private PaymentResponse pending() {
        return PaymentResponse.builder()
                .orderId(36L).method("VNPay").amount(new BigDecimal("45000"))
                .status("PENDING")
                .paymentUrl("http://localhost:8082/mock-gateway/pay?orderId=36")
                .qrCodeUrl("http://localhost:8082/mock-gateway/pay?orderId=36")
                .build();
    }

    private PaymentResponse success() {
        return PaymentResponse.builder()
                .id(5L).orderId(36L).method("VNPay").amount(new BigDecimal("45000"))
                .status("SUCCESS").transactionRef("MOCK-1")
                .build();
    }

    // ===== POST /request =====

    @Test
    void request_valid_returns201WithUrl() throws Exception {
        when(paymentService.requestPayment(any(PaymentRequest.class))).thenReturn(pending());

        mvc.perform(post("/api/v1/payments/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":36,\"method\":\"VNPay\",\"idempotencyKey\":\"k1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.paymentUrl").value("http://localhost:8082/mock-gateway/pay?orderId=36"))
            .andExpect(jsonPath("$.amount").value(45000));
    }

    @Test
    void create_alias_alsoReturns201() throws Exception {
        when(paymentService.requestPayment(any(PaymentRequest.class))).thenReturn(pending());

        mvc.perform(post("/api/v1/payments/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":36,\"method\":\"VNPay\",\"idempotencyKey\":\"k1\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void request_missingIdempotencyKey_returns400() throws Exception {
        mvc.perform(post("/api/v1/payments/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":36,\"method\":\"VNPay\"}"))
            .andExpect(status().isBadRequest());
        verify(paymentService, never()).requestPayment(any());
    }

    @Test
    void request_missingOrderId_returns400() throws Exception {
        mvc.perform(post("/api/v1/payments/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"VNPay\",\"idempotencyKey\":\"k1\"}"))
            .andExpect(status().isBadRequest());
        verify(paymentService, never()).requestPayment(any());
    }

    @Test
    void request_invalidMethod_returns400() throws Exception {
        mvc.perform(post("/api/v1/payments/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":36,\"method\":\"ABC\",\"idempotencyKey\":\"k1\"}"))
            .andExpect(status().isBadRequest());
    }

    // ===== POST /notify (webhook của cổng) =====

    private static final String NOTIFY_BODY =
            "{\"orderId\":36,\"transactionId\":\"MOCK-1\",\"status\":\"SUCCESS\","
          + "\"idempotencyKey\":\"k1\",\"method\":\"VNPay\"}";

    @Test
    void notify_withoutSecret_returns403() throws Exception {
        mvc.perform(post("/api/v1/payments/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(NOTIFY_BODY))
            .andExpect(status().isForbidden());
        verify(paymentService, never()).handleNotify(any());
    }

    @Test
    void notify_wrongSecret_returns403() throws Exception {
        mvc.perform(post("/api/v1/payments/notify")
                .header("X-Gateway-Secret", "sai-roi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(NOTIFY_BODY))
            .andExpect(status().isForbidden());
        verify(paymentService, never()).handleNotify(any());
    }

    @Test
    void notify_correctSecret_returns200() throws Exception {
        when(paymentService.handleNotify(any(PaymentNotifyRequest.class))).thenReturn(success());

        mvc.perform(post("/api/v1/payments/notify")
                .header("X-Gateway-Secret", "mock-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(NOTIFY_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.transactionRef").value("MOCK-1"));
    }

    @Test
    void notify_blankStatus_returns400() throws Exception {
        mvc.perform(post("/api/v1/payments/notify")
                .header("X-Gateway-Secret", "mock-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":36,\"status\":\"\",\"idempotencyKey\":\"k1\",\"method\":\"VNPay\"}"))
            .andExpect(status().isBadRequest());
        verify(paymentService, never()).handleNotify(any());
    }

    // ===== GET =====

    @Test
    void getPayment_returns200() throws Exception {
        when(paymentService.getPayment(5L)).thenReturn(success());

        mvc.perform(get("/api/v1/payments/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getPaymentsByOrder_returnsList() throws Exception {
        when(paymentService.getPaymentsByOrder(36L)).thenReturn(List.of(success()));

        mvc.perform(get("/api/v1/payments/order/36"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].orderId").value(36));
    }

    @Test
    void getPaymentsByOrder_empty_returnsEmptyList() throws Exception {
        when(paymentService.getPaymentsByOrder(40L)).thenReturn(List.of());

        mvc.perform(get("/api/v1/payments/order/40"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}