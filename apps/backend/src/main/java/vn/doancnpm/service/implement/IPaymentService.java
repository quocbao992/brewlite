package vn.doancnpm.service.implement;

import java.time.LocalDateTime;
import java.util.List;
//import java.util.Map;
//import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
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
import vn.doancnpm.service.PaymentService;

@Service
@RequiredArgsConstructor
@Transactional
public class IPaymentService implements PaymentService {
    private static final String PENDING = "PENDING";
    //private static final String PROCESSING = "PROCESSING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String REFUNDED = "REFUNDED";

   /* private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        PENDING, Set.of(PROCESSING, SUCCESS, FAILED),
        PROCESSING, Set.of(SUCCESS, FAILED),
        SUCCESS, Set.of(REFUNDED),
        FAILED, Set.of(),
        REFUNDED, Set.of()
    );*/

    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;                // chỉ dùng để ĐỌC đơn hàng
    private final CheckoutService checkoutService;          // SUCCESS -> confirmOrder (PAID, trừ kho, trừ điểm) 
    private final OrderService orderService;                // FAILED -> hủy đơn
    private final PaymentGatewayService paymentGatewayService;            // cổng (mock)

     // Bước "requestPayment" trong sơ đồ: tạo giao dịch PENDING rồi xin URL/QR từ cổng
   /* @Override
    public PaymentResponse requestPayment(PaymentRequest rq) {
        PaymentResponse res = createPayment(rq);
        if (PENDING.equals(res.getStatus())) {
            String url = paymentGatewayService.createPaymentUrl(res.getId(), res.getOrderId(), res.getAmount());
            res.setPaymentUrl(url);
            res.setQrCodeUrl(url);
        }
        return res;
    }*/

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse requestPayment(PaymentRequest rq) {
        if (rq.getIdempotencyKey() == null || rq.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey không được để trống");
        }
        if (rq.getMethod() == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không được để trống");
        }
 
        // 1. Key đã có kết quả (SUCCESS/FAILED) thì trả bản ghi cũ
        var existing = paymentRepo.findByIdempotencyKey(rq.getIdempotencyKey());
        if (existing.isPresent()) {
            Payment old = existing.get();
            if (!old.getOrder().getId().equals(rq.getOrderId())) {
                throw new IllegalStateException("idempotencyKey đã được dùng cho đơn hàng khác");
            }
            return mapToPaymentResponse(old);
        }
 
        // 2. Kiểm tra nghiệp vụ trên đơn hàng
        Order order = orderRepo.findById(rq.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
 
        if (order.getStatus() != Order.Status.PENDING
                && order.getStatus() != Order.Status.PAYMENT_FAILED) {
            throw new IllegalStateException("Chỉ thanh toán được đơn PENDING hoặc PAYMENT_FAILED");
        }
        if (paymentRepo.existsByOrderIdAndStatus(order.getId(), SUCCESS)) {
            throw new IllegalStateException("Đơn hàng đã được thanh toán");
        }
        
        String url = paymentGatewayService.createPaymentUrl(
            order.getId(), order.getTotalAmount(), rq.getMethod().name(), rq.getIdempotencyKey());
        
        // 3. Tạo Payment: số tiền lấy từ Order, không nhận từ client
        return PaymentResponse.builder()
                .orderId(order.getId())
                .method(rq.getMethod().name())
                .amount(order.getTotalAmount())
                .status(PENDING)
                .paymentUrl(url)
                .qrCodeUrl(url)
                .build();
                

        //return mapToPaymentResponse(paymentRepo.save(payment));
    }

    // Cổng gọi về: NotifyPayment(orderId, transactionId, status)
    /*@Override
    public PaymentResponse handleNotify(PaymentNotifyRequest rq) {
        List<Payment> payments = paymentRepo.findByOrderIdOrderByCreatedAtDesc(rq.getOrderId());
        if (payments.isEmpty()) {
            throw new IllegalArgumentException("Không có giao dịch thanh toán cho đơn hàng này");
        }*/

    // Cổng báo về: lúc này mới tạo Payment (SUCCESS hoặc FAILED)
    @Override
    public PaymentResponse handleNotify(PaymentNotifyRequest rq) {
        String target = rq.getStatus() == null ? "" : rq.getStatus().trim().toUpperCase();
        if (!SUCCESS.equals(target) && !FAILED.equals(target)) {
            throw new IllegalArgumentException("Trạng thái từ cổng không hợp lệ: " + rq.getStatus());
        }

        // Giao dịch mới nhất của đơn. Báo trùng cùng trạng thái thì updateStatus bỏ qua.
        //return updateStatus(payments.get(0).getId(), rq.getStatus(), rq.getTransactionId());
    //}
 
    


// Báo trùng: cùng key thì không xử lý lần hai
        var existing = paymentRepo.findByIdempotencyKey(rq.getIdempotencyKey());
        if (existing.isPresent()) {
            Payment old = existing.get();
            if (!old.getOrder().getId().equals(rq.getOrderId())) {
                throw new IllegalStateException("idempotencyKey đã được dùng cho đơn hàng khác");
            }
            if (!old.getStatus().equals(target)) {
                throw new IllegalStateException("Không thể chuyển trạng thái " + old.getStatus() + " -> " + target);
            }
            return mapToPaymentResponse(old);
        }

        Order order = orderRepo.findById(rq.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (order.getStatus() != Order.Status.PENDING
                && order.getStatus() != Order.Status.PAYMENT_FAILED) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ thanh toán");
        }
 
        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .order(order)
                .method(rq.getMethod())
                .amount(order.getTotalAmount())
                .status(target)
                .idempotencyKey(rq.getIdempotencyKey())
                .transactionRef(rq.getTransactionId())
                .createdAt(now)
                .paidAt(SUCCESS.equals(target) ? now : null)
                .build();
        Payment saved = paymentRepo.save(payment);  
        
        if (SUCCESS.equals(target)) {
            checkoutService.confirmOrder(order.getId());          // PAID, trừ kho, trừ điểm
        } else {
            orderService.cancel(order.getId(), "Thanh toán thất bại");
        }
        return mapToPaymentResponse(saved);
    }

    /*@Override
    public PaymentResponse updateStatus(Long paymentId, String newStatus, String transactionRef) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán"));
 
        String current = payment.getStatus();
        String target =(newStatus == null || newStatus.isBlank()) ? null : newStatus.trim().toUpperCase();
        boolean changed = target != null && !current.equals(target);

        if (changed) {
            if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
                throw new IllegalStateException("Không thể chuyển trạng thái " + current + " -> " + target);
            }
 
            payment.setStatus(target);
            if (SUCCESS.equals(target)) {
                payment.setPaidAt(LocalDateTime.now());
            }
        }

        if (transactionRef != null && !transactionRef.isBlank()) {
            payment.setTransactionRef(transactionRef);
        }
        
        Payment saved = paymentRepo.save(payment);

        // Chỉ tác động sang Order khi trạng thái payment VỪA đổi (tránh trừ kho/điểm hai lần)
        if (changed) {
            Order order = saved.getOrder();
            if (SUCCESS.equals(target)) {
                checkoutService.confirmOrder(order.getId());   // -> PAID, trừ kho, trừ điểm, đóng giỏ
            } else if (FAILED.equals(target) && (order.getStatus() == Order.Status.PENDING
                || order.getStatus() == Order.Status.PAYMENT_FAILED)){
                orderService.cancel(order.getId(),"Thanh toán thất bại");
            }
        }
        return mapToPaymentResponse(payment);
    }
 
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán"));
        return mapToPaymentResponse(payment);
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrder(Long orderId) {
        return paymentRepo.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }*/

    @Override
    public PaymentResponse updateStatus(Long paymentId, String newStatus, String transactionRef) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán"));
        String target = newStatus == null ? "" : newStatus.trim().toUpperCase();
        if (!REFUNDED.equals(target) || !SUCCESS.equals(payment.getStatus())) {
            throw new IllegalStateException("Không thể chuyển trạng thái " + payment.getStatus() + " -> " + target);
        }
        payment.setStatus(REFUNDED);
        return mapToPaymentResponse(paymentRepo.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán"));
        return mapToPaymentResponse(payment);
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrder(Long orderId) {
        return paymentRepo.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }
 
    private PaymentResponse mapToPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .method(p.getMethod().name())
                .amount(p.getAmount())
                .status(p.getStatus())
                .transactionRef(p.getTransactionRef())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}    
