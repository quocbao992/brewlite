package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.LoyaltyTransaction;
import vn.doancnpm.domain.Order;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.LoyaltyTransactionRepository;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.service.implement.ILoyaltyService;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock OrderRepository orderRepo;
    @Mock CustomerRepository customerRepo;
    @Mock LoyaltyTransactionRepository txRepo;

    @InjectMocks ILoyaltyService service;

    // ===== Hàm hỗ trợ =====

    private Customer customer(Long id, int points) {
        Customer c = new Customer();
        ReflectionTestUtils.setField(c, "id", id);
        c.setLoyaltyP(points);
        return c;
    }

    private Order order(Long id, Order.Status status, String total, int pointsUsed, Customer c) {
        Order o = mock(Order.class);
        lenient().when(o.getId()).thenReturn(id);
        lenient().when(o.getStatus()).thenReturn(status);
        lenient().when(o.getTotalAmount()).thenReturn(new BigDecimal(total));
        lenient().when(o.getLoyaltyPointsUsed()).thenReturn(pointsUsed);
        lenient().when(o.getCustomer()).thenReturn(c);
        return o;
    }

    private LoyaltyTransaction tx(String type, int points) {
        return LoyaltyTransaction.builder().type(type).points(points).build();
    }

    // ===== calculatePoints / calculateDiscountFromPoints =====

    @Test
    void calculatePoints_roundsDown() {
        assertThat(service.calculatePoints(new BigDecimal("115000"))).isEqualTo(11);
        assertThat(service.calculatePoints(new BigDecimal("10000"))).isEqualTo(1);
        assertThat(service.calculatePoints(new BigDecimal("9999"))).isEqualTo(0);
    }

    @Test
    void calculatePoints_null_returnsZero() {
        assertThat(service.calculatePoints(null)).isEqualTo(0);
    }

    @Test
    void calculateDiscountFromPoints_1000dPerPoint() {
        assertThat(service.calculateDiscountFromPoints(5)).isEqualByComparingTo("5000");
        assertThat(service.calculateDiscountFromPoints(0)).isEqualByComparingTo("0");
    }

    // ===== validateRedeem =====

    @Test
    void validateRedeem_pointsZeroOrNegative_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.validateRedeem(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> service.validateRedeem(1L, -3));
    }

    @Test
    void validateRedeem_customerNotFound_throws() {
        when(customerRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.validateRedeem(1L, 5));
    }

    @Test
    void validateRedeem_notEnoughPoints_throws() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L, 3)));
        assertThrows(IllegalStateException.class, () -> service.validateRedeem(1L, 5));
    }

    @Test
    void validateRedeem_enoughPoints_ok() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L, 5)));
        service.validateRedeem(1L, 5);   // đúng bằng số điểm hiện có vẫn hợp lệ
    }

    // ===== redeemPoints =====

    @Test
    void redeemPoints_success_deductsPointsAndSavesRedeem() {
        Customer c = customer(1L, 60);
        Order o = order(10L, Order.Status.PAID, "45000", 5, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(customerRepo.findById(1L)).thenReturn(Optional.of(c));

        int result = service.redeemPoints(10L);

        assertThat(result).isEqualTo(5);
        assertThat(c.getLoyaltyP()).isEqualTo(55);
        verify(customerRepo).save(c);
        ArgumentCaptor<LoyaltyTransaction> cap = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(txRepo).save(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo("REDEEM");
        assertThat(cap.getValue().getPoints()).isEqualTo(5);
    }

    @Test
    void redeemPoints_orderWithoutPoints_returnsZeroAndSavesNothing() {
        Customer c = customer(1L, 60);
        Order o = order(10L, Order.Status.PAID, "45000", 0, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));

        assertThat(service.redeemPoints(10L)).isEqualTo(0);
        verify(txRepo, never()).save(any(LoyaltyTransaction.class));
        verify(customerRepo, never()).save(any(Customer.class));
    }

    @Test
    void redeemPoints_alreadyRedeemed_throws() {
        Customer c = customer(1L, 60);
        Order o = order(10L, Order.Status.PAID, "45000", 5, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(txRepo.existsByOrderIdAndType(10L, "REDEEM")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.redeemPoints(10L));
        assertThat(c.getLoyaltyP()).isEqualTo(60);             // không trừ lần hai
        verify(txRepo, never()).save(any(LoyaltyTransaction.class));
    }

    @Test
    void redeemPoints_notEnoughPoints_throwsAndKeepsBalance() {
        Customer c = customer(1L, 3);
        Order o = order(10L, Order.Status.PAID, "45000", 5, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(customerRepo.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(IllegalStateException.class, () -> service.redeemPoints(10L));
        assertThat(c.getLoyaltyP()).isEqualTo(3);
        verify(txRepo, never()).save(any(LoyaltyTransaction.class));
    }

    @Test
    void redeemPoints_orderNotFound_throws() {
        when(orderRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.redeemPoints(10L));
    }

    // ===== earnPoints =====

    @Test
    void earnPoints_completedOrder_addsPointsAndSavesEarn() {
        Customer c = customer(1L, 50);
        Order o = order(10L, Order.Status.COMPLETED, "115000", 5, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));

        int result = service.earnPoints(10L);

        assertThat(result).isEqualTo(11);
        assertThat(c.getLoyaltyP()).isEqualTo(61);
        ArgumentCaptor<LoyaltyTransaction> cap = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(txRepo).save(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo("EARN");
        assertThat(cap.getValue().getPoints()).isEqualTo(11);
    }

    @Test
    void earnPoints_orderNotCompleted_throws() {
        Order o = order(10L, Order.Status.PAID, "115000", 0, customer(1L, 50));
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        assertThrows(IllegalStateException.class, () -> service.earnPoints(10L));
        verify(txRepo, never()).save(any(LoyaltyTransaction.class));
    }

    @Test
    void earnPoints_alreadyEarned_throws() {
        Customer c = customer(1L, 50);
        Order o = order(10L, Order.Status.COMPLETED, "115000", 0, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(txRepo.existsByOrderIdAndType(10L, "EARN")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.earnPoints(10L));
        assertThat(c.getLoyaltyP()).isEqualTo(50);             // không cộng lần hai
    }

    @Test
    void earnPoints_amountTooSmall_returnsZeroAndSavesNothing() {
        Customer c = customer(1L, 50);
        Order o = order(10L, Order.Status.COMPLETED, "5000", 0, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));

        assertThat(service.earnPoints(10L)).isEqualTo(0);
        verify(txRepo, never()).save(any(LoyaltyTransaction.class));
        assertThat(c.getLoyaltyP()).isEqualTo(50);
    }

    // ===== revertPoints =====

    @Test
    void revertPoints_orderNotCancelled_throws() {
        Order o = order(10L, Order.Status.PAID, "45000", 5, customer(1L, 55));
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        assertThrows(IllegalStateException.class, () -> service.revertPoints(10L));
    }

    @Test
    void revertPoints_onlyRedeemed_givesPointsBack() {
        Customer c = customer(1L, 55);
        Order o = order(10L, Order.Status.CANCELLED, "45000", 5, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(txRepo.findByOrderIdAndType(10L, "EARN")).thenReturn(Optional.empty());   // THÊM
        when(txRepo.findByOrderIdAndType(10L, "REDEEM")).thenReturn(Optional.of(tx("REDEEM", 5)));

        int net = service.revertPoints(10L);

        assertThat(net).isEqualTo(5);
        assertThat(c.getLoyaltyP()).isEqualTo(60);
        ArgumentCaptor<LoyaltyTransaction> cap = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(txRepo).save(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo("REDEEM_REVERT");
        assertThat(cap.getValue().getPoints()).isEqualTo(5);
    }

    @Test
    void revertPoints_earnedAndRedeemed_returnsNetChange() {
        Customer c = customer(1L, 61);
        Order o = order(10L, Order.Status.CANCELLED, "115000", 5, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(txRepo.findByOrderIdAndType(10L, "EARN")).thenReturn(Optional.of(tx("EARN", 11)));
        when(txRepo.findByOrderIdAndType(10L, "REDEEM")).thenReturn(Optional.of(tx("REDEEM", 5)));

        int net = service.revertPoints(10L);

        assertThat(net).isEqualTo(-6);                          // -11 thu hồi + 5 trả lại
        assertThat(c.getLoyaltyP()).isEqualTo(55);
        verify(txRepo, times(2)).save(any(LoyaltyTransaction.class));
    }

    @Test
    void revertPoints_alreadyReverted_doesNothing() {
        Customer c = customer(1L, 60);
        Order o = order(10L, Order.Status.CANCELLED, "45000", 5, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(txRepo.findByOrderIdAndType(10L, "EARN")).thenReturn(Optional.empty());   // THÊM
        when(txRepo.findByOrderIdAndType(10L, "REDEEM")).thenReturn(Optional.of(tx("REDEEM", 5)));
        when(txRepo.existsByOrderIdAndType(10L, "REDEEM_REVERT")).thenReturn(true);

        assertThat(service.revertPoints(10L)).isEqualTo(0);
        assertThat(c.getLoyaltyP()).isEqualTo(60);              // không hoàn lần hai
        verify(txRepo, never()).save(any(LoyaltyTransaction.class));
    }

    @Test
    void revertPoints_noPointsInvolved_returnsZero() {
        Customer c = customer(1L, 60);
        Order o = order(10L, Order.Status.CANCELLED, "45000", 0, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));

        assertThat(service.revertPoints(10L)).isEqualTo(0);
        verify(txRepo, never()).save(any(LoyaltyTransaction.class));
    }

    @Test
    void revertPoints_balanceNeverGoesNegative() {
        Customer c = customer(1L, 5);                           // khách đã tiêu bớt điểm thưởng
        Order o = order(10L, Order.Status.CANCELLED, "115000", 0, c);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(o));
        when(txRepo.findByOrderIdAndType(10L, "EARN")).thenReturn(Optional.of(tx("EARN", 11)));

        service.revertPoints(10L);

        assertThat(c.getLoyaltyP()).isEqualTo(0);
    }
}