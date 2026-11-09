package vn.doancnpm.service;

import java.util.List;

import vn.doancnpm.dto.Request.PaymentNotifyRequest;
import vn.doancnpm.dto.Request.PaymentRequest;
import vn.doancnpm.dto.Response.PaymentResponse;

public interface PaymentService {

    //PaymentResponse createPayment(PaymentRequest rq);
    PaymentResponse requestPayment(PaymentRequest rq);     // Tạo giao dịch + trả QR/URL
    PaymentResponse handleNotify(PaymentNotifyRequest rq); // Cổng báo kết quả 
    PaymentResponse updateStatus(Long paymentId, String newStatus, String transactionRef);
    PaymentResponse getPayment(Long paymentId);
    List<PaymentResponse> getPaymentsByOrder(Long orderId);
}
