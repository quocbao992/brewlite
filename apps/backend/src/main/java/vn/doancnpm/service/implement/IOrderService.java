package vn.doancnpm.service.implement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.OrderItem;
import vn.doancnpm.domain.OrderItemTopping;
import vn.doancnpm.dto.Response.AdminOrderResponse;
import vn.doancnpm.dto.Response.OrderItemResponse;
import vn.doancnpm.dto.Response.OrderItemToppingResponse;
import vn.doancnpm.dto.Response.OrderResponse;
import vn.doancnpm.dto.Response.OrderSummaryResponse;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.service.OrderService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.service.ToppingInventoryService;
import vn.doancnpm.service.LoyaltyService;
@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
@Transactional
public class IOrderService implements OrderService {

    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final ProductInventoryService productInventoryService;
    private final ToppingInventoryService toppingInventoryService;
    private final LoyaltyService loyaltyService;

    // ===================== Lịch sử đơn (có phân trang) =====================
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getHistory(Long customerId, Order.Status status,
            int page, int size) {
        Customer customer = customerRepo.findById(customerId)
                .or(() -> customerRepo.findByUserId(customerId))
                .orElse(null);
        Long targetId = customer != null ? customer.getId() : customerId;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> result = (status == null)
                ? orderRepo.findByCustomerId(targetId, pageable)
                : orderRepo.findByCustomerIdAndStatus(targetId, status, pageable);
        return PageResponse.of(result.map(this::mapToSummary));
    }

    // ===================== Chi tiết đơn =====================
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        return mapToOrderResponse(findOrder(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByCode(String orderCode) {
        Order o = orderRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng " + orderCode));
        return mapToOrderResponse(o);
    }

    // ===================== Hàng đợi Barista =====================
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getQueue(List<Order.Status> statuses) {
        return orderRepo.findByStatusInOrderByCreatedAtAsc(statuses)
                .stream().map(this::mapToOrderResponse).toList();
    }

    // ===================== Cập nhật trạng thái =====================
    @Override
    public OrderResponse updateStatus(Long orderId, Order.Status newStatus, String reason) {
        if (newStatus == Order.Status.CANCELLED) {
            String r = (reason == null || reason.isBlank()) ? "Nhân viên hủy đơn" : reason;
            return cancel(orderId, r);
        }
        Order order = findOrder(orderId);
        if (!canChange(order.getStatus(), newStatus)) {
            throw new IllegalStateException(
                    "Không thể chuyển đơn từ " + order.getStatus() + " sang " + newStatus);
        }
        order.setStatus(newStatus);
        Order saved = orderRepo.save(order);
        if (newStatus == Order.Status.COMPLETED) {
            loyaltyService.earnPoints(orderId);   // cộng điểm thưởng
        }
        return mapToOrderResponse(saved);
    }

    // ===================== Hủy đơn (Chỉ đơn chưa thanh toán PENDING / PAYMENT_FAILED) =====================
    @Override
    public OrderResponse cancel(Long orderId, String reason) {
        Order order = findOrder(orderId);
        Order.Status st = order.getStatus();
        if (st != Order.Status.PENDING && st != Order.Status.PAYMENT_FAILED) {
            throw new IllegalStateException("Đơn hàng đã thanh toán không thể hủy");
        }
        order.setStatus(Order.Status.CANCELLED);
        order.setCancelReason(reason);
        Order saved = orderRepo.save(order);
        loyaltyService.revertPoints(orderId);   // trả lại điểm khách đã dùng nếu có
        return mapToOrderResponse(saved);
    }

    // ===================== Admin: lọc + phân trang =====================
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> adminSearch(LocalDate fromDate, LocalDate toDate,
            Order.Status status, int page, int size) {

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (fromDate != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"),
                        fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                ps.add(cb.lessThan(root.<LocalDateTime>get("createdAt"),
                        toDate.plusDays(1).atStartOfDay()));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
          //  if (paymentMethod != null) {
            //    ps.add(cb.equal(root.get("paymentMethod"), paymentMethod));
          //  }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(orderRepo.findAll(spec, pageable).map(this::mapToAdmin));
    }

    // ===================== Hàm phụ =====================
    private Order findOrder(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng " + orderId));
    }

    // PENDING -> PAID -> PREPARING -> READY -> COMPLETED
    // PENDING -> PAYMENT_FAILED -> PAID; hủy qua cancel()
    private boolean canChange(Order.Status from, Order.Status to) {
        return switch (from) {
            case PENDING -> to == Order.Status.PAID || to == Order.Status.PAYMENT_FAILED;
            case PAYMENT_FAILED -> to == Order.Status.PAID;
            case PAID -> to == Order.Status.PREPARING;
            case PREPARING -> to == Order.Status.READY;
            case READY -> to == Order.Status.COMPLETED;
            default -> false; // COMPLETED, CANCELLED là trạng thái cuối
        };
    }

    private OrderSummaryResponse mapToSummary(Order o) {
        int totalItems = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        List<OrderItemResponse> itemResponses = (o.getItems() == null)
                ? Collections.emptyList()
                : o.getItems().stream().map(this::mapToItemResponse).toList();
        return OrderSummaryResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .status(o.getStatus())
                .orderType(o.getOrderType())
                .tableNumber(o.getTableNumber())
                .items(itemResponses)
                .totalAmount(o.getTotalAmount())
                .totalItems(totalItems)
                .createdAt(o.getCreatedAt())
                .build();
    }

    private AdminOrderResponse mapToAdmin(Order o) {
        return AdminOrderResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .customerName(o.getCustomer().getUser().getFullName()) // đổi theo tên field thật của Customer
                .phone(o.getCustomer().getPhone())           // đổi theo tên field thật của Customer
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private OrderItemToppingResponse mapToToppingResponse(OrderItemTopping t) {
        return OrderItemToppingResponse.builder()
                .toppingId(t.getTopping().getId())
                .name(t.getToppingName())
                .quantity(t.getQuantity())
                .unitPrice(t.getUnitPrice())
                .build();
    }

    private OrderItemResponse mapToItemResponse(OrderItem i) {
        List<OrderItemToppingResponse> toppings = (i.getToppings() == null)
                ? Collections.emptyList()
                : i.getToppings().stream().map(this::mapToToppingResponse).toList();
        String image = i.getProductImage() != null
                ? i.getProductImage()
                : (i.getProduct() != null ? i.getProduct().getImgURL() : null);
        return OrderItemResponse.builder()
                .id(i.getId())
                .productId(i.getProduct().getId())
                .productName(i.getProductName())
                .productImage(image)
                .size(i.getSize())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.getLineTotal())
                .toppings(toppings)
                .build();
    }

    private OrderResponse mapToOrderResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .customerId(o.getCustomer().getId())
                .status(o.getStatus())
                .orderType(o.getOrderType())
                .tableNumber(o.getTableNumber())
                .note(o.getNote())
                .items(o.getItems().stream().map(this::mapToItemResponse).toList())
                .subtotal(o.getSubtotal())
                .discountAmount(o.getDiscountAmount())
                .loyaltyPointsUsed(o.getLoyaltyPointsUsed())
                .totalAmount(o.getTotalAmount())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}