package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.OrderItem;
import vn.doancnpm.domain.OrderItemTopping;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Response.AdminOrderResponse;
import vn.doancnpm.dto.Response.OrderItemResponse;
import vn.doancnpm.dto.Response.OrderResponse;
import vn.doancnpm.dto.Response.OrderSummaryResponse;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.service.LoyaltyService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.service.ToppingInventoryService;
import vn.doancnpm.service.implement.IOrderService;


@SuppressWarnings({ "unchecked", "rawtypes" })
@ExtendWith(MockitoExtension.class)
class IOrderServiceTest {

    @Mock private OrderRepository orderRepo;
    @Mock private CustomerRepository customerRepo;
    @Mock private ProductInventoryService productInventoryService;
    @Mock private ToppingInventoryService toppingInventoryService;
    @Mock private LoyaltyService loyaltyService;

    @InjectMocks
    private IOrderService orderService;

    @Captor private ArgumentCaptor<Pageable> pageableCaptor;
    @Captor private ArgumentCaptor<Specification<Order>> specCaptor;

    // ===================== helpers =====================

    private static Customer customer(long id) {
        User user = new User();
        user.setId(100L + id);
        user.setFullName("Nguyễn Văn A");
        return Customer.builder().id(id).user(user).phone("0901234567").build();
    }

    private static Product product(long id, String imgUrl) {
        return Product.builder().id(id).name("Cà phê sữa").imgURL(imgUrl)
                .unitPrice(new BigDecimal("30000")).build();
    }

    /** Đơn có 1 dòng: cà phê M x2 (30.000) + 1 topping trân châu (5.000) => lineTotal 70.000. */
    private static Order order(long id, Order.Status status) {
        Order o = new Order();
        o.setId(id);
        o.setOrderCode("BL20261109" + String.format("%05d", id));
        o.setCustomer(customer(1L));
        o.setStatus(status);
        o.setOrderType(Order.OrderType.DINE_IN);
        o.setTableNumber("A1");
        o.setNote("ít đá");
        o.setSubtotal(new BigDecimal("70000"));
        o.setDiscountAmount(new BigDecimal("7000"));
        o.setLoyaltyPointsUsed(0);
        o.setTotalAmount(new BigDecimal("63000"));
        o.setCreatedAt(LocalDateTime.of(2026, 11, 9, 10, 30));
        o.setUpdatedAt(LocalDateTime.of(2026, 11, 9, 10, 45));

        OrderItem item = new OrderItem();
        item.setId(500L + id);
        item.setOrder(o);
        item.setProduct(product(1L, "coffee.jpg"));
        item.setProductName("Cà phê sữa");
        item.setProductImage("snapshot.jpg");
        item.setSize("M");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("30000"));
        item.setLineTotal(new BigDecimal("70000"));

        OrderItemTopping ot = new OrderItemTopping();
        ot.setOrderItem(item);
        ot.setTopping(Topping.builder().id(7L).name("Trân châu").unitPrice(new BigDecimal("5000")).build());
        ot.setToppingName("Trân châu");
        ot.setQuantity(1);
        ot.setUnitPrice(new BigDecimal("5000"));
        item.getToppings().add(ot);

        o.getItems().add(item);
        return o;
    }

    private void givenOrderExists(Order o) {
        when(orderRepo.findById(o.getId())).thenReturn(Optional.of(o));
    }

    // ===================== getHistory =====================

    @Test
    void getHistory_customerFoundById_noStatus_usesFindByCustomerId() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L)));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order(1, Order.Status.PAID)), pageable, 1);
        when(orderRepo.findByCustomerId(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<OrderSummaryResponse> res = orderService.getHistory(1L, null, 0, 10);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getPage()).isZero();
        assertThat(res.getSize()).isEqualTo(10);
        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getTotalPages()).isEqualTo(1);
        verify(orderRepo, never()).findByCustomerIdAndStatus(anyLong(), any(), any(Pageable.class));
    }

    @Test
    void getHistory_sortsByCreatedAtDescending_withGivenPageAndSize() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L)));
        when(orderRepo.findByCustomerId(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getHistory(1L, null, 2, 5);

        verify(orderRepo).findByCustomerId(org.mockito.ArgumentMatchers.eq(1L), pageableCaptor.capture());
        Pageable p = pageableCaptor.getValue();
        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getPageSize()).isEqualTo(5);
        assertThat(p.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getHistory_withStatus_usesFindByCustomerIdAndStatus() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L)));
        when(orderRepo.findByCustomerIdAndStatus(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(Order.Status.COMPLETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order(3, Order.Status.COMPLETED))));

        PageResponse<OrderSummaryResponse> res = orderService.getHistory(1L, Order.Status.COMPLETED, 0, 10);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getStatus()).isEqualTo(Order.Status.COMPLETED);
        verify(orderRepo, never()).findByCustomerId(anyLong(), any(Pageable.class));
    }

    @Test
    void getHistory_idIsActuallyUserId_resolvesToRealCustomerId() {
        when(customerRepo.findById(5L)).thenReturn(Optional.empty());
        when(customerRepo.findByUserId(5L)).thenReturn(Optional.of(customer(20L)));
        when(orderRepo.findByCustomerId(org.mockito.ArgumentMatchers.eq(20L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getHistory(5L, null, 0, 10);

        verify(orderRepo).findByCustomerId(org.mockito.ArgumentMatchers.eq(20L), any(Pageable.class));
    }

    @Test
    void getHistory_customerNotFound_fallsBackToRawIdWithoutThrowing() {
        when(customerRepo.findById(99L)).thenReturn(Optional.empty());
        when(customerRepo.findByUserId(99L)).thenReturn(Optional.empty());
        when(orderRepo.findByCustomerId(org.mockito.ArgumentMatchers.eq(99L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<OrderSummaryResponse> res = orderService.getHistory(99L, null, 0, 10);

        assertThat(res.getContent()).isEmpty();
        assertThat(res.getTotalElements()).isZero();
    }

    @Test
    void getHistory_mapsSummaryFields_includingTotalItemsAndItems() {
        Order o1 = order(1, Order.Status.PAID);
        Order o2 = order(2, Order.Status.READY);
        o2.getItems().get(0).setQuantity(3);
        o2.setTotalAmount(new BigDecimal("100000"));
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L)));
        when(orderRepo.findByCustomerId(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o1, o2), PageRequest.of(0, 10), 2));

        PageResponse<OrderSummaryResponse> res = orderService.getHistory(1L, null, 0, 10);

        OrderSummaryResponse s1 = res.getContent().get(0);
        assertThat(s1.getId()).isEqualTo(1L);
        assertThat(s1.getOrderCode()).isEqualTo("BL2026110900001");
        assertThat(s1.getStatus()).isEqualTo(Order.Status.PAID);
        assertThat(s1.getOrderType()).isEqualTo(Order.OrderType.DINE_IN);
        assertThat(s1.getTableNumber()).isEqualTo("A1");
        assertThat(s1.getTotalAmount()).isEqualByComparingTo("63000");
        assertThat(s1.getTotalItems()).isEqualTo(2);
        assertThat(s1.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 9, 10, 30));
        assertThat(s1.getItems()).hasSize(1);
        assertThat(s1.getItems().get(0).getProductName()).isEqualTo("Cà phê sữa");

        assertThat(res.getContent().get(1).getTotalItems()).isEqualTo(3);
    }

    @Test
    void getHistory_totalItemsSumsQuantityOfAllLines() {
        Order o = order(1, Order.Status.PAID);
        OrderItem second = new OrderItem();
        second.setProduct(product(2L, null));
        second.setProductName("Trà đào");
        second.setSize("L");
        second.setQuantity(4);
        second.setUnitPrice(new BigDecimal("20000"));
        second.setLineTotal(new BigDecimal("80000"));
        o.getItems().add(second);
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L)));
        when(orderRepo.findByCustomerId(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o)));

        PageResponse<OrderSummaryResponse> res = orderService.getHistory(1L, null, 0, 10);

        assertThat(res.getContent().get(0).getTotalItems()).isEqualTo(6); // 2 + 4
        assertThat(res.getContent().get(0).getItems()).hasSize(2);
    }

    @Test
    void getHistory_pagingMetadataIsCopiedFromPage() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer(1L)));
        Pageable pageable = PageRequest.of(1, 5);
        Page<Order> page = new PageImpl<>(List.of(order(6, Order.Status.PAID)), pageable, 11);
        when(orderRepo.findByCustomerId(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<OrderSummaryResponse> res = orderService.getHistory(1L, null, 1, 5);

        assertThat(res.getPage()).isEqualTo(1);
        assertThat(res.getSize()).isEqualTo(5);
        assertThat(res.getTotalElements()).isEqualTo(11);
        assertThat(res.getTotalPages()).isEqualTo(3);
    }

    @Test
    void getHistory_negativePage_throwsIllegalArgument() {
        assertThatThrownBy(() -> orderService.getHistory(1L, null, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(orderRepo);
    }

    @Test
    void getHistory_sizeZero_throwsIllegalArgument() {
        assertThatThrownBy(() -> orderService.getHistory(1L, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(orderRepo);
    }

    // ===================== getById / getByCode =====================

    @Test
    void getById_found_mapsAllFields() {
        Order o = order(42, Order.Status.PAID);
        o.setLoyaltyPointsUsed(7);
        givenOrderExists(o);

        OrderResponse res = orderService.getById(42L);

        assertThat(res.getId()).isEqualTo(42L);
        assertThat(res.getOrderCode()).isEqualTo("BL2026110900042");
        assertThat(res.getCustomerId()).isEqualTo(1L);
        assertThat(res.getStatus()).isEqualTo(Order.Status.PAID);
        assertThat(res.getOrderType()).isEqualTo(Order.OrderType.DINE_IN);
        assertThat(res.getTableNumber()).isEqualTo("A1");
        assertThat(res.getNote()).isEqualTo("ít đá");
        assertThat(res.getSubtotal()).isEqualByComparingTo("70000");
        assertThat(res.getDiscountAmount()).isEqualByComparingTo("7000");
        assertThat(res.getLoyaltyPointsUsed()).isEqualTo(7);
        assertThat(res.getTotalAmount()).isEqualByComparingTo("63000");
        assertThat(res.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 9, 10, 30));
        assertThat(res.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 9, 10, 45));
    }

    @Test
    void getById_mapsItemsAndToppings() {
        givenOrderExists(order(42, Order.Status.PAID));

        OrderResponse res = orderService.getById(42L);

        assertThat(res.getItems()).hasSize(1);
        OrderItemResponse item = res.getItems().get(0);
        assertThat(item.getId()).isEqualTo(542L);
        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getProductName()).isEqualTo("Cà phê sữa");
        assertThat(item.getSize()).isEqualTo("M");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("30000");
        assertThat(item.getLineTotal()).isEqualByComparingTo("70000");
        assertThat(item.getToppings()).hasSize(1);
        assertThat(item.getToppings().get(0).getToppingId()).isEqualTo(7L);
        assertThat(item.getToppings().get(0).getName()).isEqualTo("Trân châu");
        assertThat(item.getToppings().get(0).getQuantity()).isEqualTo(1);
        assertThat(item.getToppings().get(0).getUnitPrice()).isEqualByComparingTo("5000");
    }

    @Test
    void getById_prefersSnapshotImageOverProductImage() {
        givenOrderExists(order(42, Order.Status.PAID)); // productImage = snapshot.jpg, product.imgURL = coffee.jpg

        assertThat(orderService.getById(42L).getItems().get(0).getProductImage()).isEqualTo("snapshot.jpg");
    }

    @Test
    void getById_snapshotImageMissing_fallsBackToProductImage() {
        Order o = order(42, Order.Status.PAID);
        o.getItems().get(0).setProductImage(null);
        givenOrderExists(o);

        assertThat(orderService.getById(42L).getItems().get(0).getProductImage()).isEqualTo("coffee.jpg");
    }

    @Test
    void getById_noImageAnywhere_returnsNullImage() {
        Order o = order(42, Order.Status.PAID);
        o.getItems().get(0).setProductImage(null);
        o.getItems().get(0).setProduct(product(1L, null));
        givenOrderExists(o);

        assertThat(orderService.getById(42L).getItems().get(0).getProductImage()).isNull();
    }

    @Test
    void getById_orderWithoutItems_returnsEmptyItemList() {
        Order o = order(42, Order.Status.PENDING);
        o.getItems().clear();
        givenOrderExists(o);

        assertThat(orderService.getById(42L).getItems()).isEmpty();
    }

    @Test
    void getById_notFound_throwsIllegalArgument() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy đơn hàng 99");
    }

    @Test
    void getByCode_found_returnsMappedOrder() {
        when(orderRepo.findByOrderCode("BL2026110900042")).thenReturn(Optional.of(order(42, Order.Status.READY)));

        OrderResponse res = orderService.getByCode("BL2026110900042");

        assertThat(res.getId()).isEqualTo(42L);
        assertThat(res.getOrderCode()).isEqualTo("BL2026110900042");
        assertThat(res.getStatus()).isEqualTo(Order.Status.READY);
    }

    @Test
    void getByCode_notFound_throwsIllegalArgumentWithCode() {
        when(orderRepo.findByOrderCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getByCode("NOPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy đơn hàng NOPE");
    }

    // ===================== getQueue =====================

    @Test
    void getQueue_returnsMappedOrdersInRepositoryOrder() {
        List<Order.Status> statuses = List.of(Order.Status.PAID, Order.Status.PREPARING);
        when(orderRepo.findByStatusInOrderByCreatedAtAsc(statuses))
                .thenReturn(List.of(order(1, Order.Status.PAID), order(2, Order.Status.PREPARING)));

        List<OrderResponse> res = orderService.getQueue(statuses);

        assertThat(res).extracting(OrderResponse::getId).containsExactly(1L, 2L);
        assertThat(res).extracting(OrderResponse::getStatus)
                .containsExactly(Order.Status.PAID, Order.Status.PREPARING);
    }

    @Test
    void getQueue_emptyQueue_returnsEmptyList() {
        List<Order.Status> statuses = List.of(Order.Status.READY);
        when(orderRepo.findByStatusInOrderByCreatedAtAsc(statuses)).thenReturn(List.of());

        assertThat(orderService.getQueue(statuses)).isEmpty();
    }

    // ===================== updateStatus: chuyển trạng thái hợp lệ =====================

    @ParameterizedTest(name = "{0} -> {1} hợp lệ")
    @CsvSource({
            "PENDING,PAID",
            "PENDING,PAYMENT_FAILED",
            "PAYMENT_FAILED,PAID",
            "PAID,PREPARING",
            "PREPARING,READY",
            "READY,COMPLETED"
    })
    void updateStatus_validTransition_savesNewStatus(Order.Status from, Order.Status to) {
        Order o = order(1, from);
        givenOrderExists(o);
        when(orderRepo.save(o)).thenReturn(o);

        OrderResponse res = orderService.updateStatus(1L, to, null);

        assertThat(res.getStatus()).isEqualTo(to);
        assertThat(o.getStatus()).isEqualTo(to);
        verify(orderRepo).save(o);
        verifyNoInteractions(productInventoryService, toppingInventoryService);
    }

    @Test
    void updateStatus_toCompleted_earnsLoyaltyPoints() {
        Order o = order(1, Order.Status.READY);
        givenOrderExists(o);
        when(orderRepo.save(o)).thenReturn(o);

        orderService.updateStatus(1L, Order.Status.COMPLETED, null);

        InOrder inOrder = inOrder(orderRepo, loyaltyService);
        inOrder.verify(orderRepo).save(o);
        inOrder.verify(loyaltyService).earnPoints(1L);
    }

    @ParameterizedTest(name = "chuyển sang {1} không cộng điểm")
    @CsvSource({ "PENDING,PAID", "PAID,PREPARING", "PREPARING,READY" })
    void updateStatus_notCompleted_doesNotEarnPoints(Order.Status from, Order.Status to) {
        Order o = order(1, from);
        givenOrderExists(o);
        when(orderRepo.save(o)).thenReturn(o);

        orderService.updateStatus(1L, to, null);

        verify(loyaltyService, never()).earnPoints(anyLong());
    }

    // ===================== updateStatus: chuyển trạng thái KHÔNG hợp lệ =====================

    private static final Set<String> VALID_TRANSITIONS = Set.of(
            "PENDING>PAID", "PENDING>PAYMENT_FAILED", "PAYMENT_FAILED>PAID",
            "PAID>PREPARING", "PREPARING>READY", "READY>COMPLETED");

    /** Toàn bộ cặp (from, to) trừ các cặp hợp lệ và trừ to = CANCELLED (đi qua cancel()). */
    static Stream<Arguments> invalidTransitions() {
        return Arrays.stream(Order.Status.values()).flatMap(from -> Arrays.stream(Order.Status.values())
                .filter(to -> to != Order.Status.CANCELLED)
                .filter(to -> !VALID_TRANSITIONS.contains(from + ">" + to))
                .map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1} bị từ chối")
    @MethodSource("invalidTransitions")
    void updateStatus_invalidTransition_throwsIllegalStateAndDoesNotSave(Order.Status from, Order.Status to) {
        givenOrderExists(order(1, from));

        assertThatThrownBy(() -> orderService.updateStatus(1L, to, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Không thể chuyển đơn từ " + from + " sang " + to);

        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(loyaltyService);
    }

    @Test
    void updateStatus_orderNotFound_throwsIllegalArgument() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(99L, Order.Status.PAID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy đơn hàng 99");
    }

    // ===================== updateStatus -> CANCELLED (uỷ quyền cho cancel) =====================

    @Test
    void updateStatus_toCancelled_withReason_cancelsWithThatReason() {
        Order o = order(1, Order.Status.PENDING);
        givenOrderExists(o);
        when(orderRepo.save(o)).thenReturn(o);

        OrderResponse res = orderService.updateStatus(1L, Order.Status.CANCELLED, "Hết nguyên liệu");

        assertThat(res.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(o.getCancelReason()).isEqualTo("Hết nguyên liệu");
        verify(loyaltyService).revertPoints(1L);
    }

    @ParameterizedTest(name = "lý do ''{0}'' -> dùng lý do mặc định của nhân viên")
    @ValueSource(strings = { "", "   " })
    void updateStatus_toCancelled_blankReason_usesDefaultStaffReason(String blank) {
        Order o = order(1, Order.Status.PENDING);
        givenOrderExists(o);
        when(orderRepo.save(o)).thenReturn(o);

        orderService.updateStatus(1L, Order.Status.CANCELLED, blank);

        assertThat(o.getCancelReason()).isEqualTo("Nhân viên hủy đơn");
    }

    @Test
    void updateStatus_toCancelled_nullReason_usesDefaultStaffReason() {
        Order o = order(1, Order.Status.PAYMENT_FAILED);
        givenOrderExists(o);
        when(orderRepo.save(o)).thenReturn(o);

        orderService.updateStatus(1L, Order.Status.CANCELLED, null);

        assertThat(o.getCancelReason()).isEqualTo("Nhân viên hủy đơn");
    }

    @Test
    void updateStatus_toCancelled_onPaidOrder_isRejected() {
        givenOrderExists(order(1, Order.Status.PAID));

        assertThatThrownBy(() -> orderService.updateStatus(1L, Order.Status.CANCELLED, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Đơn hàng đã thanh toán không thể hủy");

        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(loyaltyService);
    }

    // ===================== cancel =====================

    @ParameterizedTest(name = "hủy đơn đang {0}")
    @EnumSource(value = Order.Status.class, names = { "PENDING", "PAYMENT_FAILED" })
    void cancel_unpaidOrder_setsCancelledAndReason_thenRevertsPoints(Order.Status from) {
        Order o = order(1, from);
        givenOrderExists(o);
        when(orderRepo.save(o)).thenReturn(o);

        OrderResponse res = orderService.cancel(1L, "Khách đổi ý");

        assertThat(res.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(o.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(o.getCancelReason()).isEqualTo("Khách đổi ý");

        InOrder inOrder = inOrder(orderRepo, loyaltyService);
        inOrder.verify(orderRepo).save(o);
        inOrder.verify(loyaltyService).revertPoints(1L); // hoàn điểm SAU khi đã lưu trạng thái CANCELLED
    }

    @ParameterizedTest(name = "không thể hủy đơn đang {0}")
    @EnumSource(value = Order.Status.class, names = { "PAID", "PREPARING", "READY", "COMPLETED", "CANCELLED" })
    void cancel_paidOrBeyond_throwsIllegalState_andChangesNothing(Order.Status from) {
        Order o = order(1, from);
        givenOrderExists(o);

        assertThatThrownBy(() -> orderService.cancel(1L, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Đơn hàng đã thanh toán không thể hủy");

        assertThat(o.getStatus()).isEqualTo(from);
        assertThat(o.getCancelReason()).isNull();
        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(loyaltyService);
    }

    @Test
    void cancel_orderNotFound_throwsIllegalArgument() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancel(99L, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy đơn hàng 99");

        verifyNoInteractions(loyaltyService);
    }

    // ===================== adminSearch =====================

    /** Bộ mock Criteria API để chạy thử Specification mà service tạo ra. */
    private static class SpecHarness {
        final Root<Order> root = mock(Root.class);
        final CriteriaQuery<?> query = mock(CriteriaQuery.class);
        final CriteriaBuilder cb = mock(CriteriaBuilder.class);
        final Path<LocalDateTime> createdAtPath = mock(Path.class);
        final Path<Order.Status> statusPath = mock(Path.class);

        SpecHarness() {
            lenient().doReturn(createdAtPath).when(root).get("createdAt");
            lenient().doReturn(statusPath).when(root).get("status");
        }

        Predicate expectFrom(LocalDateTime bound) {
            Predicate p = mock(Predicate.class);
            when(cb.greaterThanOrEqualTo(createdAtPath, bound)).thenReturn(p);
            return p;
        }

        Predicate expectBefore(LocalDateTime bound) {
            Predicate p = mock(Predicate.class);
            when(cb.lessThan(createdAtPath, bound)).thenReturn(p);
            return p;
        }

        Predicate expectStatus(Order.Status st) {
            Predicate p = mock(Predicate.class);
            when(cb.equal(statusPath, st)).thenReturn(p);
            return p;
        }

        void run(Specification<Order> spec) {
            spec.toPredicate(root, query, cb);
        }
    }

    private Specification<Order> captureSpecAfterAdminSearch(LocalDate from, LocalDate to, Order.Status st) {
        when(orderRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        orderService.adminSearch(from, to, st, 0, 10);
        verify(orderRepo).findAll(specCaptor.capture(), any(Pageable.class));
        return specCaptor.getValue();
    }

    @Test
    void adminSearch_mapsOrdersToAdminResponse() {
        Order o = order(7, Order.Status.PAID);
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o), pageable, 1));

        PageResponse<AdminOrderResponse> res = orderService.adminSearch(null, null, null, 0, 10);

        assertThat(res.getContent()).hasSize(1);
        AdminOrderResponse a = res.getContent().get(0);
        assertThat(a.getId()).isEqualTo(7L);
        assertThat(a.getOrderCode()).isEqualTo("BL2026110900007");
        assertThat(a.getCustomerName()).isEqualTo("Nguyễn Văn A");
        assertThat(a.getPhone()).isEqualTo("0901234567");
        assertThat(a.getTotalAmount()).isEqualByComparingTo("63000");
        assertThat(a.getStatus()).isEqualTo(Order.Status.PAID);
        assertThat(a.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 9, 10, 30));
        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getTotalPages()).isEqualTo(1);
    }

    @Test
    void adminSearch_sortsByCreatedAtDescending_withGivenPaging() {
        when(orderRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.adminSearch(null, null, null, 3, 20);

        verify(orderRepo).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable p = pageableCaptor.getValue();
        assertThat(p.getPageNumber()).isEqualTo(3);
        assertThat(p.getPageSize()).isEqualTo(20);
        assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void adminSearch_emptyResult_returnsEmptyPage() {
        when(orderRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<AdminOrderResponse> res = orderService.adminSearch(null, null, null, 0, 10);

        assertThat(res.getContent()).isEmpty();
        assertThat(res.getTotalElements()).isZero();
    }

    @Test
    void adminSearch_negativePage_throwsIllegalArgument() {
        assertThatThrownBy(() -> orderService.adminSearch(null, null, null, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(orderRepo);
    }

    @Test
    void adminSearch_noFilters_specificationAddsNoPredicate() {
        Specification<Order> spec = captureSpecAfterAdminSearch(null, null, null);
        SpecHarness h = new SpecHarness();

        h.run(spec);

        verify(h.cb).and(new Predicate[0]);
        verifyNoMoreInteractions(h.cb);
    }

    @Test
    void adminSearch_allFilters_specificationBuildsFromToStatusPredicatesInOrder() {
        Specification<Order> spec = captureSpecAfterAdminSearch(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9), Order.Status.PAID);
        SpecHarness h = new SpecHarness();
        // fromDate: bao gồm 00:00 của ngày bắt đầu; toDate: loại trừ 00:00 của NGÀY HÔM SAU => gồm trọn ngày toDate
        Predicate from = h.expectFrom(LocalDateTime.of(2026, 11, 1, 0, 0));
        Predicate before = h.expectBefore(LocalDateTime.of(2026, 11, 10, 0, 0));
        Predicate st = h.expectStatus(Order.Status.PAID);

        h.run(spec);

        verify(h.cb).and(new Predicate[] { from, before, st });
    }

    @Test
    void adminSearch_onlyFromDate_specificationHasOnlyLowerBound() {
        Specification<Order> spec = captureSpecAfterAdminSearch(LocalDate.of(2026, 11, 1), null, null);
        SpecHarness h = new SpecHarness();
        Predicate from = h.expectFrom(LocalDateTime.of(2026, 11, 1, 0, 0));

        h.run(spec);

        verify(h.cb).and(new Predicate[] { from });
    }

    @Test
    void adminSearch_onlyToDate_specificationHasOnlyExclusiveNextDayBound() {
        Specification<Order> spec = captureSpecAfterAdminSearch(null, LocalDate.of(2026, 12, 31), null);
        SpecHarness h = new SpecHarness();
        // 31/12 + 1 ngày = 01/01 năm sau (kiểm tra biên qua tháng/năm)
        Predicate before = h.expectBefore(LocalDateTime.of(2027, 1, 1, 0, 0));

        h.run(spec);

        verify(h.cb).and(new Predicate[] { before });
    }

    @Test
    void adminSearch_onlyStatus_specificationHasOnlyStatusEquality() {
        Specification<Order> spec = captureSpecAfterAdminSearch(null, null, Order.Status.READY);
        SpecHarness h = new SpecHarness();
        Predicate st = h.expectStatus(Order.Status.READY);

        h.run(spec);

        verify(h.cb).and(new Predicate[] { st });
    }

    @Test
    void adminSearch_sameDayRange_coversWholeDay() {
        LocalDate day = LocalDate.of(2026, 11, 9);
        Specification<Order> spec = captureSpecAfterAdminSearch(day, day, null);
        SpecHarness h = new SpecHarness();
        Predicate from = h.expectFrom(LocalDateTime.of(2026, 11, 9, 0, 0));
        Predicate before = h.expectBefore(LocalDateTime.of(2026, 11, 10, 0, 0));

        h.run(spec);

        verify(h.cb).and(new Predicate[] { from, before });
    }
}
