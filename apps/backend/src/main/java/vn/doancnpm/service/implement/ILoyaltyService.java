package vn.doancnpm.service.implement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.LoyaltyTransaction;
import vn.doancnpm.domain.Order;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.LoyaltyTransactionRepository;
import vn.doancnpm.repository.OrderRepository;
//import vn.doancnpm.repository.PaymentRepository;
import vn.doancnpm.service.LoyaltyService;

@Service
@RequiredArgsConstructor
@Transactional
public class ILoyaltyService implements LoyaltyService {

    // Quy tắc ví dụ: 10.000đ = 1 điểm (cả nhóm cần chốt)
    private static final BigDecimal AMOUNT_PER_POINT = new BigDecimal("10000");
    private static final BigDecimal VALUE_PER_POINT = new BigDecimal("1000");

    private static final String EARN = "EARN";
    private static final String REVERT = "REVERT";
    private static final String REDEEM = "REDEEM";
    private static final String REDEEM_REVERT = "REDEEM_REVERT";

    private final OrderRepository orderRepo;        // chỉ ĐỌC
    private final CustomerRepository customerRepo;
    private final LoyaltyTransactionRepository txRepo;

    @Override
    public int calculatePoints(BigDecimal totalAmount) {
        if (totalAmount == null) {
            return 0;
        }
        return totalAmount.divide(AMOUNT_PER_POINT, 0, RoundingMode.DOWN).intValue();
    }

    @Override
    public BigDecimal calculateDiscountFromPoints(int points) {
        return VALUE_PER_POINT.multiply(BigDecimal.valueOf(points));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRedeem(Long customerId, int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Số điểm quy đổi phải lớn hơn 0");
        }
        Customer c = customerRepo.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        if (c.getLoyaltyP() < points) {
            throw new IllegalStateException("Khách hàng không đủ điểm để quy đổi");
        }
    }

    // Trừ điểm khách đã dùng cho đơn (gọi khi đơn chuyển sang PAID)
    @Override
    public int redeemPoints(Long orderId) {
        Order order = findOrder(orderId);

        int points = order.getLoyaltyPointsUsed();
        if (points <= 0) {
            return 0;                                   // đơn không dùng điểm
        }
        if (txRepo.existsByOrderIdAndType(orderId, REDEEM)) {
            throw new IllegalStateException("Đơn hàng này đã trừ điểm quy đổi");
        }
        validateRedeem(order.getCustomer().getId(), points);

        changePoints(order.getCustomer(), -points);
        saveTransaction(order, points, REDEEM);
        return points;
    }

    // Cộng điểm thưởng (gọi khi đơn chuyển sang COMPLETED)
    @Override
    public int earnPoints(Long orderId) {
        Order order = findOrder(orderId);
                //.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != Order.Status.COMPLETED) {
        throw new IllegalStateException("Chỉ cộng điểm khi đơn hàng đã hoàn tất");
        }
        if (txRepo.existsByOrderIdAndType(orderId, EARN)) {
        throw new IllegalStateException("Đơn hàng này đã được cộng điểm");
    }

        int points = calculatePoints(order.getTotalAmount());
        if (points > 0) {
            changePoints(order.getCustomer(), points);
            saveTransaction(order, points, EARN);
        }
        return points;
    }

    // Hoàn điểm khi đơn bị hủy. Trả về thay đổi điểm ròng của khách (dương = khách được trả lại)
    @Override
    public int revertPoints(Long orderId) {
         Order order = findOrder(orderId);

        if (order.getStatus() != Order.Status.CANCELLED) {
            throw new IllegalStateException("Chỉ hoàn điểm khi đơn hàng đã bị hủy");
        }

        int net = 0;

        // 1. Thu hồi điểm thưởng (nếu từng cộng và chưa thu hồi)
        var earned = txRepo.findByOrderIdAndType(orderId, EARN);
        if (earned.isPresent() && !txRepo.existsByOrderIdAndType(orderId, REVERT)) {
            int p = earned.get().getPoints();
            changePoints(order.getCustomer(), -p);
            saveTransaction(order, p, REVERT);
            net -= p;
        }

        // 2. Trả lại điểm khách đã dùng (nếu từng trừ và chưa trả)
        var redeemed = txRepo.findByOrderIdAndType(orderId, REDEEM);
        if (redeemed.isPresent() && !txRepo.existsByOrderIdAndType(orderId, REDEEM_REVERT)) {
            int p = redeemed.get().getPoints();
            changePoints(order.getCustomer(), p);
            saveTransaction(order, p, REDEEM_REVERT);
            net += p;
        }

        return net;     // đơn không có điểm nào thì trả 0, không báo lỗi
    }

    // ===================== Hàm phụ =====================
    private Order findOrder(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }

    // delta dương = cộng, âm = trừ (không để điểm bị âm)
    private void changePoints(Customer customer, int delta) {
        customer.setLoyaltyP(Math.max(0, customer.getLoyaltyP() + delta));
        customerRepo.save(customer);
    }

    private void saveTransaction(Order order, int points, String type) {
        txRepo.save(LoyaltyTransaction.builder()
                .customer(order.getCustomer())
                .order(order)
                .points(points)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build());
    }
}