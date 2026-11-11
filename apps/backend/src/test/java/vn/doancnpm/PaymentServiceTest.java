package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Payment;
import vn.doancnpm.dto.Request.PaymentNotifyRequest;
import vn.doancnpm.dto.Request.PaymentRequest;
import vn.doancnpm.dto.Response.PaymentResponse;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.repository.PaymentRepository;
import vn.doancnpm.service.CheckoutService;
import vn.doancnpm.service.OrderService;
import vn.doancnpm.service.PaymentGatewayService;
import vn.doancnpm.service.implement.IPaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepo;
    @Mock OrderRepository orderRepo;
    @Mock CheckoutService checkoutService;
    @Mock OrderService orderService;
    @Mock PaymentGatewayService paymentGatewayService;

    @InjectMocks IPaymentService service;

    // ===== Hàm hỗ trợ =====

    private Order order(Long id, Order.Status status, String total) {
        Order o = mock(Order.class);
        lenient().when(o.getId()).thenReturn(id);
        lenient().when(o.getStatus()).thenReturn(status);
        lenient().when(o.getTotalAmount()).thenReturn(new BigDecimal(total));
        return o;
    }

    private PaymentRequest request(Long orderId, Payment.Method method, String key) {
        PaymentRequest rq = new PaymentRequest();
        rq.setOrderId(orderId);
        rq.setMethod(method);
        rq.setIdempotencyKey(key);
        return rq;
    }

    private PaymentNotifyRequest notify(Long orderId, String status, String key) {
        return new PaymentNotifyRequest(orderId, "MOCK-1", status, key, Payment.Method.VNPay);
    }

    private Payment payment(Order o, String status, String key) {
        return Payment.builder()
                .order(o)
                .method(Payment.Method.VNPay)
                .amount(new BigDecimal("45000"))
                .status(status)
                .idempotencyKey(key)
                .transactionRef("MOCK-OLD")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== requestPayment =====

    @Test
    void requestPayment_returnsUrlAndDoesNotSaveToDb() {
        Order o = order(36L, Order.Status.PENDING, "45000");
        when(orderRepo.findById(36L)).thenReturn(Optional.of(o));
        when(paymentGatewayService.createPaymentUrl(eq(36L), any(BigDecimal.class), eq("VNPay"), eq("k1")))
                .thenReturn("http://localhost:8082/mock-gateway/pay?orderId=36");

        PaymentResponse res = service.requestPayment(request(36L, Payment.Method.VNPay, "k1"));

        assertThat(res.getStatus()).isEqualTo("PENDING");
        assertThat(res.getId()).isNull();
        assertThat(res.getOrderId()).isEqualTo(36L);
        assertThat(res.getAmount()).isEqualByComparingTo("45000");
        assertThat(res.getPaymentUrl()).contains("orderId=36");
        assertThat(res.getQrCodeUrl()).isNotNull();
        verify(paymentRepo, never()).save(any(Payment.class));   // KHÔNG lưu PENDING
    }

    @Test
    void requestPayment_blankIdempotencyKey_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.requestPayment(request(36L, Payment.Method.VNPay, " ")));
    }

    @Test
    void requestPayment_nullMethod_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.requestPayment(request(36L, null, "k1")));
    }

    @Test
    void requestPayment_orderNotFound_throws() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.requestPayment(request(99L, Payment.Method.VNPay, "k1")));
    }

    @Test
    void requestPayment_orderAlreadyPaid_throws() {
        Order o = order(36L, Order.Status.PAID, "45000");
        when(orderRepo.findById(36L)).thenReturn(Optional.of(o));
        assertThrows(IllegalStateException.class,
                () -> service.requestPayment(request(36L, Payment.Method.VNPay, "k1")));
    }

    @Test
    void requestPayment_orderAlreadyHasSuccessPayment_throws() {
        Order o = order(36L, Order.Status.PENDING, "45000");
        when(orderRepo.findById(36L)).thenReturn(Optional.of(o));
        when(paymentRepo.existsByOrderIdAndStatus(36L, "SUCCESS")).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> service.requestPayment(request(36L, Payment.Method.VNPay, "k1")));
    }

    @Test
    void requestPayment_existingKey_returnsOldPaymentWithoutGateway() {
        Order o = order(36L, Order.Status.PAID, "45000");
        Payment old = payment(o, "SUCCESS", "k1");
        when(paymentRepo.findByIdempotencyKey("k1")).thenReturn(Optional.of(old));

        PaymentResponse res = service.requestPayment(request(36L, Payment.Method.VNPay, "k1"));

        assertThat(res.getStatus()).isEqualTo("SUCCESS");
        assertThat(res.getPaymentUrl()).isNull();
        verify(paymentGatewayService, never()).createPaymentUrl(any(), any(), any(), any());
    }

    @Test
    void requestPayment_sameKeyForOtherOrder_throws() {
        Order other = order(99L, Order.Status.PAID, "10000");
        when(paymentRepo.findByIdempotencyKey("k1"))
                .thenReturn(Optional.of(payment(other, "SUCCESS", "k1")));
        assertThrows(IllegalStateException.class,
                () -> service.requestPayment(request(36L, Payment.Method.VNPay, "k1")));
    }

    // ===== handleNotify =====

    @Test
    void handleNotify_success_savesPaymentAndConfirmsOrder() {
        Order o = order(36L, Order.Status.PENDING, "45000");
        when(orderRepo.findById(36L)).thenReturn(Optional.of(o));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = service.handleNotify(notify(36L, "success", "k1"));  // chữ thường vẫn được

        ArgumentCaptor<Payment> cap = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepo).save(cap.capture());
        Payment saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getPaidAt()).isNotNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("45000");   // lấy từ Order
        assertThat(saved.getIdempotencyKey()).isEqualTo("k1");
        assertThat(saved.getTransactionRef()).isEqualTo("MOCK-1");
        assertThat(res.getStatus()).isEqualTo("SUCCESS");
        verify(checkoutService).confirmOrder(36L);
        verify(orderService, never()).cancel(any(), any());
    }

    @Test
    void handleNotify_failed_savesFailedAndCancelsOrder() {
        Order o = order(36L, Order.Status.PENDING, "45000");
        when(orderRepo.findById(36L)).thenReturn(Optional.of(o));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = service.handleNotify(notify(36L, "FAILED", "k1"));

        ArgumentCaptor<Payment> cap = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(cap.getValue().getPaidAt()).isNull();
        assertThat(res.getStatus()).isEqualTo("FAILED");
        verify(orderService).cancel(eq(36L), any(String.class));
        verify(checkoutService, never()).confirmOrder(any());
    }

    @Test
    void handleNotify_invalidStatus_throwsAndSavesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> service.handleNotify(notify(36L, "PENDING", "k1")));
        verify(paymentRepo, never()).save(any(Payment.class));
    }

    @Test
    void handleNotify_duplicateSameStatus_returnsOldAndDoesNothing() {
        Order o = order(36L, Order.Status.PAID, "45000");
        when(paymentRepo.findByIdempotencyKey("k1"))
                .thenReturn(Optional.of(payment(o, "SUCCESS", "k1")));

        PaymentResponse res = service.handleNotify(notify(36L, "SUCCESS", "k1"));

        assertThat(res.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepo, never()).save(any(Payment.class));
        verify(checkoutService, never()).confirmOrder(any());      // không trừ kho/điểm lần 2
    }

    @Test
    void handleNotify_duplicateDifferentStatus_throws() {
        Order o = order(36L, Order.Status.CANCELLED, "45000");
        when(paymentRepo.findByIdempotencyKey("k1"))
                .thenReturn(Optional.of(payment(o, "FAILED", "k1")));

        assertThrows(IllegalStateException.class,
                () -> service.handleNotify(notify(36L, "SUCCESS", "k1")));
        verify(checkoutService, never()).confirmOrder(any());
    }

    @Test
    void handleNotify_keyUsedForOtherOrder_throws() {
        Order other = order(99L, Order.Status.PAID, "10000");
        when(paymentRepo.findByIdempotencyKey("k1"))
                .thenReturn(Optional.of(payment(other, "SUCCESS", "k1")));

        assertThrows(IllegalStateException.class,
                () -> service.handleNotify(notify(36L, "SUCCESS", "k1")));
    }

    @Test
    void handleNotify_orderNotWaitingPayment_throwsAndSavesNothing() {
        Order o = order(36L, Order.Status.PAID, "45000");
        when(orderRepo.findById(36L)).thenReturn(Optional.of(o));

        assertThrows(IllegalStateException.class,
                () -> service.handleNotify(notify(36L, "SUCCESS", "k2")));
        verify(paymentRepo, never()).save(any(Payment.class));
        verify(checkoutService, never()).confirmOrder(any());
    }

    @Test
    void handleNotify_orderNotFound_throws() {
        when(orderRepo.findById(36L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.handleNotify(notify(36L, "SUCCESS", "k1")));
    }

    // ===== updateStatus (chỉ dùng hoàn tiền) =====

    @Test
    void updateStatus_successToRefunded_ok() {
        Order o = order(36L, Order.Status.CANCELLED, "45000");
        Payment p = payment(o, "SUCCESS", "k1");
        when(paymentRepo.findById(5L)).thenReturn(Optional.of(p));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse res = service.updateStatus(5L, "refunded", null);

        assertThat(res.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void updateStatus_failedToSuccess_throws() {
        Order o = order(36L, Order.Status.CANCELLED, "45000");
        when(paymentRepo.findById(5L)).thenReturn(Optional.of(payment(o, "FAILED", "k1")));
        assertThrows(IllegalStateException.class, () -> service.updateStatus(5L, "SUCCESS", null));
    }

    @Test
    void updateStatus_paymentNotFound_throws() {
        when(paymentRepo.findById(5L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(5L, "REFUNDED", null));
    }

    // ===== đọc dữ liệu =====

    @Test
    void getPaymentsByOrder_returnsMappedList() {
        Order o = order(36L, Order.Status.PAID, "45000");
        when(paymentRepo.findByOrderIdOrderByCreatedAtDesc(36L))
                .thenReturn(List.of(payment(o, "SUCCESS", "k1")));

        List<PaymentResponse> list = service.getPaymentsByOrder(36L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(list.get(0).getOrderId()).isEqualTo(36L);
    }
}