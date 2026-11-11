package vn.doancnpm.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.domain.Order;
import vn.doancnpm.dto.Request.CancelOrderRequest;
import vn.doancnpm.dto.Request.CheckoutRequest;
import vn.doancnpm.dto.Request.UpdateOrderStatusRequest;
import vn.doancnpm.dto.Response.OrderResponse;
import vn.doancnpm.dto.Response.OrderSummaryResponse;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.service.CheckoutService;
import vn.doancnpm.service.OrderService;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest rq) {
        return ResponseEntity.ok(checkoutService.checkout(rq));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> history(
            @RequestParam Long customerId,
            @RequestParam(required = false) Order.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getHistory(customerId, status, page, size));
    }

    @GetMapping("/queue")
    public ResponseEntity<List<OrderResponse>> queue(
            @RequestParam(required = false) List<Order.Status> status) {
        List<Order.Status> statuses = (status == null || status.isEmpty())
                ? List.of(Order.Status.PAID, Order.Status.PREPARING, Order.Status.READY)
                : status;
        return ResponseEntity.ok(orderService.getQueue(statuses));
    }

    
    @GetMapping("/code/{orderCode}")
    public ResponseEntity<OrderResponse> byCode(@PathVariable String orderCode) {
        return ResponseEntity.ok(orderService.getByCode(orderCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest rq) {
        if (rq.getStatus() == Order.Status.PAID) {
            throw new IllegalStateException("Không đặt PAID trực tiếp, hãy dùng thanh toán/xác nhận");
        }
        return ResponseEntity.ok(orderService.updateStatus(id, rq.getStatus(), rq.getReason()));
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest rq) {
        String reason = (rq != null && rq.getReason() != null && !rq.getReason().isBlank())
                ? rq.getReason()
                : "Khách hàng yêu cầu hủy qua ứng dụng";
        return ResponseEntity.ok(orderService.cancel(id, reason));
    }

    // TẠM để test bằng Postman (PaymentService sẽ gọi confirmOrder khi thanh toán xong)
    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(checkoutService.confirmOrder(id));
    }
}
