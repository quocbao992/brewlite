package vn.doancnpm.service;

import java.math.BigDecimal;

public interface LoyaltyService {
    int calculatePoints(BigDecimal totalAmount);               // tiền -> điểm thưởng
    BigDecimal calculateDiscountFromPoints(int points);        // điểm -> tiền giảm
    void validateRedeem(Long customerId, int points);          // kiểm tra đủ điểm
    int redeemPoints(Long orderId);                            // trừ điểm khi đơn PAID
    int earnPoints(Long orderId);                              // cộng điểm khi đơn COMPLETED
    int revertPoints(Long orderId);                            // hoàn điểm khi đơn CANCELLED
}
