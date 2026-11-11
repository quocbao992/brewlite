package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.doancnpm.domain.Cart;
import vn.doancnpm.domain.CartItem;
import vn.doancnpm.domain.CartItemTopping;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.OrderItem;
import vn.doancnpm.domain.OrderItemTopping;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.CheckoutRequest;
import vn.doancnpm.dto.Response.OrderResponse;
import vn.doancnpm.repository.CartItemRepository;
import vn.doancnpm.repository.CartRepository;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.service.LoyaltyService;
import vn.doancnpm.service.OrderService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.service.PromotionService;
import vn.doancnpm.service.ToppingInventoryService;
import vn.doancnpm.service.implement.ICheckoutService; 

@ExtendWith(MockitoExtension.class)
class ICheckoutServiceTest {

    @Mock private CustomerRepository customerRepo;
    @Mock private CartRepository cartRepo;
    @Mock private CartItemRepository cartItemRepo;
    @Mock private OrderRepository orderRepo;
    @Mock private OrderService orderService;
    @Mock private ProductInventoryService productInventoryService;
    @Mock private ToppingInventoryService toppingInventoryService;
    @Mock private LoyaltyService loyaltyService;
    @Mock private PromotionService promotionService;

    @InjectMocks
    private ICheckoutService checkoutService;

    private Customer customer;
    private Cart cart;
    private Product coffee;
    private Product tea;
    private Topping pearl;
    private Topping jelly;

    @BeforeEach
    void initFixtures() {
        User user = new User();
        user.setId(100L);
        user.setFullName("Nguyễn Văn A");

        customer = Customer.builder().id(1L).user(user).phone("0901234567").build();
        cart = Cart.builder().id(10L).customer(customer).build(); // status mặc định = true

        coffee = Product.builder().id(1L).name("Cà phê sữa").imgURL("coffee.jpg")
                .unitPrice(new BigDecimal("30000")).build();
        tea = Product.builder().id(2L).name("Trà đào").imgURL("tea.jpg")
                .unitPrice(new BigDecimal("20000")).build();
        pearl = Topping.builder().id(7L).name("Trân châu").unitPrice(new BigDecimal("5000")).build();
        jelly = Topping.builder().id(8L).name("Thạch").unitPrice(new BigDecimal("3000")).build();
    }

    // ===================== helpers =====================

    private CartItem cartItem(Product p, CartItem.Size size, int qty, String unitPrice, CartItemTopping... toppings) {
        return CartItem.builder()
                .cart(cart)
                .product(p)
                .size(size)
                .quantity(qty)
                .unitPrice(new BigDecimal(unitPrice))
                .toppings(new ArrayList<>(List.of(toppings)))
                .build();
    }

    private CartItemTopping cartTopping(Topping t, int qty, String price) {
        return CartItemTopping.builder().topping(t).quantity(qty).price(new BigDecimal(price)).build();
    }

    private CheckoutRequest request() {
        CheckoutRequest rq = new CheckoutRequest();
        rq.setCustomerId(1L);
        rq.setOrderType(Order.OrderType.DINE_IN);
        rq.setTableNumber("A1");
        rq.setNote("ít đá");
        return rq;
    }

    private void givenCustomerAndCart(List<CartItem> items) {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepo.findByCart(cart)).thenReturn(items);
    }

    /** lenient: không phải test nào cũng có topping nên không ép dùng hết stub. */
    private void givenStockAlwaysAvailable() {
        lenient().when(productInventoryService.checkStockAvailable(anyLong(), anyInt())).thenReturn(true);
        lenient().when(toppingInventoryService.checkStockAvailable(anyLong(), anyInt())).thenReturn(true);
    }

    private void givenSaveAssignsId(long id) {
        when(orderRepo.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(id);
            return o;
        });
    }

    private OrderResponse givenGetByIdReturns(long id, Order.Status st) {
        OrderResponse res = OrderResponse.builder().id(id).customerId(1L).status(st).build();
        when(orderService.getById(id)).thenReturn(res);
        return res;
    }

    /** So sánh BigDecimal theo giá trị (bỏ qua scale) trong matcher của Mockito. */
    private static BigDecimal sameAs(String value) {
        return argThat(x -> x != null && x.compareTo(new BigDecimal(value)) == 0);
    }

    /** Giỏ chuẩn: ly cà phê M x2 (+1 trân châu) = 70.000 và ly trà L x1 = 20.000 => subtotal 90.000. */
    private List<CartItem> standardCart() {
        return List.of(
                cartItem(coffee, CartItem.Size.M, 2, "30000", cartTopping(pearl, 1, "5000")),
                cartItem(tea, CartItem.Size.L, 1, "20000"));
    }

    private Order savedOrderOf(ArgumentCaptor<Order> captor) {
        verify(orderRepo).save(captor.capture());
        return captor.getValue();
    }

    // ===================== checkout: HAPPY PATH =====================

    @Test
    void checkout_happyPath_createsPendingOrderWithCorrectTotals() {
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(42L);
        OrderResponse expected = givenGetByIdReturns(42L, Order.Status.PENDING);

        OrderResponse result = checkoutService.checkout(request());

        assertThat(result).isSameAs(expected);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order order = savedOrderOf(captor);
        assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getOrderType()).isEqualTo(Order.OrderType.DINE_IN);
        assertThat(order.getTableNumber()).isEqualTo("A1");
        assertThat(order.getNote()).isEqualTo("ít đá");
        assertThat(order.getSubtotal()).isEqualByComparingTo("90000");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("90000");
        assertThat(order.getLoyaltyPointsUsed()).isZero();
        assertThat(order.getOrderCode()).matches("BL\\d{8}00042"); // BL + yyyyMMdd + id 5 chữ số
    }

    @Test
    void checkout_copiesItemSnapshotAndToppings_andComputesLineTotal() {
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(request());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order order = savedOrderOf(captor);
        assertThat(order.getItems()).hasSize(2);

        OrderItem first = order.getItems().get(0);
        assertThat(first.getOrder()).isSameAs(order);
        assertThat(first.getProduct()).isSameAs(coffee);
        assertThat(first.getProductName()).isEqualTo("Cà phê sữa"); // snapshot tên lúc đặt
        assertThat(first.getProductImage()).isEqualTo("coffee.jpg");
        assertThat(first.getSize()).isEqualTo("M");
        assertThat(first.getQuantity()).isEqualTo(2);
        assertThat(first.getUnitPrice()).isEqualByComparingTo("30000");
        // lineTotal = (30.000 + 5.000) x 2
        assertThat(first.getLineTotal()).isEqualByComparingTo("70000");
        assertThat(first.getToppings()).hasSize(1);
        OrderItemTopping ot = first.getToppings().get(0);
        assertThat(ot.getOrderItem()).isSameAs(first);
        assertThat(ot.getTopping()).isSameAs(pearl);
        assertThat(ot.getToppingName()).isEqualTo("Trân châu");
        assertThat(ot.getQuantity()).isEqualTo(1);
        assertThat(ot.getUnitPrice()).isEqualByComparingTo("5000");

        OrderItem second = order.getItems().get(1);
        assertThat(second.getSize()).isEqualTo("L");
        assertThat(second.getLineTotal()).isEqualByComparingTo("20000");
        assertThat(second.getToppings()).isEmpty();
    }

    @Test
    void checkout_doesNotDeductStock_doesNotCloseCart_andDoesNotTouchLoyaltyOrPromotion() {
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(request());

        verify(productInventoryService, never()).deductStock(anyLong(), any(Order.class), anyInt());
        verify(toppingInventoryService, never()).deductStock(anyLong(), any(Order.class), anyInt());
        verify(cartRepo, never()).save(any(Cart.class));
        verifyNoInteractions(promotionService);
        verifyNoInteractions(loyaltyService);
        assertThat(cart.getStatus()).isTrue();
    }

    @Test
    void checkout_checksStockForEveryProductAndTopping() {
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(request());

        verify(productInventoryService).checkStockAvailable(1L, 2);
        verify(productInventoryService).checkStockAvailable(2L, 1);
        verify(toppingInventoryService).checkStockAvailable(7L, 1);
    }

    @Test
    void checkout_sameProductOnDifferentLines_stockNeedIsMerged() {
        // cùng 1 sản phẩm nhưng khác size/topping vẫn trừ chung 1 kho
        List<CartItem> items = List.of(
                cartItem(coffee, CartItem.Size.M, 2, "30000", cartTopping(pearl, 1, "5000")),
                cartItem(coffee, CartItem.Size.L, 3, "35000", cartTopping(pearl, 2, "5000")));
        givenCustomerAndCart(items);
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(request());

        verify(productInventoryService, times(1)).checkStockAvailable(anyLong(), anyInt());
        verify(productInventoryService).checkStockAvailable(1L, 5);
        verify(toppingInventoryService, times(1)).checkStockAvailable(anyLong(), anyInt());
        verify(toppingInventoryService).checkStockAvailable(7L, 3);
    }

    @Test
    void checkout_cartItemWithNullToppings_isHandled() {
        CartItem item = cartItem(coffee, CartItem.Size.S, 3, "30000");
        item.setToppings(null);
        givenCustomerAndCart(List.of(item));
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(request());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order order = savedOrderOf(captor);
        assertThat(order.getSubtotal()).isEqualByComparingTo("90000");
        assertThat(order.getItems().get(0).getToppings()).isEmpty();
        verifyNoInteractions(toppingInventoryService);
    }

    @Test
    void checkout_multipleToppingsOnOneItem_lineTotalSumsAllToppings() {
        // (25.000 + 5.000x2 + 3.000x1) x 2 = 76.000
        CartItem item = cartItem(coffee, CartItem.Size.M, 2, "25000",
                cartTopping(pearl, 2, "5000"), cartTopping(jelly, 1, "3000"));
        givenCustomerAndCart(List.of(item));
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(request());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order order = savedOrderOf(captor);
        assertThat(order.getItems().get(0).getLineTotal()).isEqualByComparingTo("76000");
        assertThat(order.getSubtotal()).isEqualByComparingTo("76000");
        assertThat(order.getItems().get(0).getToppings()).hasSize(2);
    }

    // ===================== checkout: tìm khách / giỏ hàng =====================

    @Test
    void checkout_customerFoundByUserId_whenIdLookupFails() {
        CheckoutRequest rq = request();
        rq.setCustomerId(5L); // thực ra là userId
        when(customerRepo.findById(5L)).thenReturn(Optional.empty());
        when(customerRepo.findByUserId(5L)).thenReturn(Optional.of(customer)); // customer.id = 1
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer)); // findActiveCart tra lại bằng id thật
        when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepo.findByCart(cart)).thenReturn(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(9L);
        OrderResponse expected = givenGetByIdReturns(9L, Order.Status.PENDING);

        assertThat(checkoutService.checkout(rq)).isSameAs(expected);
    }

    @Test
    void checkout_customerNotFound_throwsIllegalArgument() {
        when(customerRepo.findById(1L)).thenReturn(Optional.empty());
        when(customerRepo.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy thông tin khách hàng");

        verifyNoInteractions(cartRepo, orderRepo);
    }

    @Test
    void checkout_noCart_throwsIllegalState() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Không có giỏ hàng đang dùng");

        verifyNoInteractions(cartItemRepo, orderRepo);
    }

    @Test
    void checkout_emptyCart_throwsIllegalState() {
        givenCustomerAndCart(List.of());

        assertThatThrownBy(() -> checkoutService.checkout(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Giỏ hàng trống");

        verifyNoInteractions(productInventoryService, toppingInventoryService, orderRepo);
    }

    @Test
    void checkout_closedCart_isReactivatedBeforeUse() {
        cart.setStatus(false); // giỏ đã checkout lần trước
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepo.save(cart)).thenReturn(cart);
        when(cartItemRepo.findByCart(cart)).thenReturn(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(request());

        assertThat(cart.getStatus()).isTrue();
        verify(cartRepo).save(cart);
    }

    // ===================== checkout: hết hàng =====================

    @Test
    void checkout_productOutOfStock_throwsAndSavesNothing() {
        givenCustomerAndCart(List.of(cartItem(coffee, CartItem.Size.M, 2, "30000")));
        when(productInventoryService.checkStockAvailable(1L, 2)).thenReturn(false);

        assertThatThrownBy(() -> checkoutService.checkout(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sản phẩm ID 1")
                .hasMessageContaining("cần 2");

        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(toppingInventoryService, promotionService, loyaltyService);
    }

    @Test
    void checkout_toppingOutOfStock_throwsAndSavesNothing() {
        givenCustomerAndCart(List.of(
                cartItem(coffee, CartItem.Size.M, 2, "30000", cartTopping(pearl, 4, "5000"))));
        when(productInventoryService.checkStockAvailable(1L, 2)).thenReturn(true);
        when(toppingInventoryService.checkStockAvailable(7L, 4)).thenReturn(false);

        assertThatThrownBy(() -> checkoutService.checkout(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Topping ID 7")
                .hasMessageContaining("cần 4");

        verify(orderRepo, never()).save(any(Order.class));
    }

    // ===================== checkout: khuyến mãi =====================

    @Test
    void checkout_withVoucher_appliesDiscount_andMarksVoucherUsedAfterSaving() {
        CheckoutRequest rq = request();
        rq.setVoucherCode("SALE10");
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        when(promotionService.calculateDiscount(org.mockito.ArgumentMatchers.eq("SALE10"), sameAs("90000")))
                .thenReturn(new BigDecimal("9000"));
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(rq);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order order = savedOrderOf(captor);
        assertThat(order.getSubtotal()).isEqualByComparingTo("90000");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("9000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("81000");

        InOrder inOrder = inOrder(orderRepo, promotionService);
        inOrder.verify(orderRepo).save(any(Order.class));
        inOrder.verify(promotionService).markUsed("SALE10");
    }

    @ParameterizedTest(name = "voucher ''{0}'' bị bỏ qua")
    @ValueSource(strings = { "", "   " })
    void checkout_blankVoucher_isIgnored(String blank) {
        CheckoutRequest rq = request();
        rq.setVoucherCode(blank);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(rq);

        verifyNoInteractions(promotionService);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        assertThat(savedOrderOf(captor).getTotalAmount()).isEqualByComparingTo("90000");
    }

    @Test
    void checkout_invalidVoucher_propagatesException_andSavesNothing() {
        CheckoutRequest rq = request();
        rq.setVoucherCode("HETHAN");
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        when(promotionService.calculateDiscount(org.mockito.ArgumentMatchers.eq("HETHAN"), any(BigDecimal.class)))
                .thenThrow(new IllegalStateException("Mã khuyến mãi đã hết hạn"));

        assertThatThrownBy(() -> checkoutService.checkout(rq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mã khuyến mãi đã hết hạn");

        verify(orderRepo, never()).save(any(Order.class));
        verify(promotionService, never()).markUsed(any());
    }

    // ===================== checkout: điểm tích lũy =====================

    @Test
    void checkout_withLoyaltyPoints_deductsPointValueFromTotal() {
        CheckoutRequest rq = request();
        rq.setLoyaltyPointsUsed(10);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        when(loyaltyService.calculateDiscountFromPoints(10)).thenReturn(new BigDecimal("10000"));
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(rq);

        verify(loyaltyService).validateRedeem(1L, 10);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order order = savedOrderOf(captor);
        assertThat(order.getLoyaltyPointsUsed()).isEqualTo(10);
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("10000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("80000");
    }

    @Test
    void checkout_voucherAndPoints_discountsAreCombined() {
        CheckoutRequest rq = request();
        rq.setVoucherCode("SALE10");
        rq.setLoyaltyPointsUsed(5);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        when(promotionService.calculateDiscount(org.mockito.ArgumentMatchers.eq("SALE10"), sameAs("90000")))
                .thenReturn(new BigDecimal("9000"));
        when(loyaltyService.calculateDiscountFromPoints(5)).thenReturn(new BigDecimal("5000"));
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(rq);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order order = savedOrderOf(captor);
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("14000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("76000");
        verify(promotionService).markUsed("SALE10");
    }

    @Test
    void checkout_pointsWorthExactlyTheOrderValue_isAllowed_totalBecomesZero() {
        CheckoutRequest rq = request();
        rq.setLoyaltyPointsUsed(90);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        when(loyaltyService.calculateDiscountFromPoints(90)).thenReturn(new BigDecimal("90000"));
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(rq);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        assertThat(savedOrderOf(captor).getTotalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void checkout_pointsWorthMoreThanOrder_throwsAndSavesNothing() {
        CheckoutRequest rq = request();
        rq.setLoyaltyPointsUsed(91);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        when(loyaltyService.calculateDiscountFromPoints(91)).thenReturn(new BigDecimal("91000"));

        assertThatThrownBy(() -> checkoutService.checkout(rq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Số điểm quy đổi vượt quá giá trị đơn hàng");

        verify(orderRepo, never()).save(any(Order.class));
    }

    @Test
    void checkout_pointsExceedOrderAfterVoucher_throws_andVoucherIsNotMarkedUsed() {
        CheckoutRequest rq = request();
        rq.setVoucherCode("SALE50");
        rq.setLoyaltyPointsUsed(50);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        when(promotionService.calculateDiscount(org.mockito.ArgumentMatchers.eq("SALE50"), sameAs("90000")))
                .thenReturn(new BigDecimal("45000")); // còn 45.000
        when(loyaltyService.calculateDiscountFromPoints(50)).thenReturn(new BigDecimal("50000"));

        assertThatThrownBy(() -> checkoutService.checkout(rq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Số điểm quy đổi vượt quá giá trị đơn hàng");

        verify(orderRepo, never()).save(any(Order.class));
        verify(promotionService, never()).markUsed(any());
    }

    @Test
    void checkout_notEnoughLoyaltyPoints_propagatesFromValidateRedeem() {
        CheckoutRequest rq = request();
        rq.setLoyaltyPointsUsed(1000);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        doThrow(new IllegalStateException("Khách hàng không đủ điểm để quy đổi"))
                .when(loyaltyService).validateRedeem(1L, 1000);

        assertThatThrownBy(() -> checkoutService.checkout(rq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Khách hàng không đủ điểm để quy đổi");

        verify(orderRepo, never()).save(any(Order.class));
        verify(loyaltyService, never()).calculateDiscountFromPoints(anyInt());
    }

    @Test
    void checkout_zeroPoints_doesNotValidateOrCalculate() {
        CheckoutRequest rq = request();
        rq.setLoyaltyPointsUsed(0);
        givenCustomerAndCart(standardCart());
        givenStockAlwaysAvailable();
        givenSaveAssignsId(1L);
        givenGetByIdReturns(1L, Order.Status.PENDING);

        checkoutService.checkout(rq);

        verifyNoInteractions(loyaltyService);
    }

    // ===================== confirmOrder =====================

    private Order orderWithItems(long id, Order.Status st) {
        Order o = new Order();
        o.setId(id);
        o.setCustomer(customer);
        o.setStatus(st);

        OrderItem oi = new OrderItem();
        oi.setOrder(o);
        oi.setProduct(coffee);
        oi.setProductName("Cà phê sữa");
        oi.setQuantity(2);

        OrderItemTopping ot = new OrderItemTopping();
        ot.setOrderItem(oi);
        ot.setTopping(pearl);
        ot.setQuantity(2);
        oi.getToppings().add(ot);

        o.getItems().add(oi);
        return o;
    }

    @ParameterizedTest(name = "confirmOrder từ trạng thái {0}")
    @EnumSource(value = Order.Status.class, names = { "PENDING", "PAYMENT_FAILED" })
    void confirmOrder_confirmableStatus_runsFullFlowInOrder(Order.Status from) {
        Order order = orderWithItems(42L, from);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        OrderResponse paid = OrderResponse.builder().id(42L).customerId(1L).status(Order.Status.PAID).build();
        when(orderService.updateStatus(42L, Order.Status.PAID, null)).thenReturn(paid);
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepo.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        OrderResponse result = checkoutService.confirmOrder(42L);

        assertThat(result).isSameAs(paid);
        InOrder inOrder = inOrder(productInventoryService, toppingInventoryService, loyaltyService,
                orderService, cartRepo);
        inOrder.verify(productInventoryService).deductStock(1L, order, 2);
        inOrder.verify(toppingInventoryService).deductStock(7L, order, 2);
        inOrder.verify(loyaltyService).redeemPoints(42L);
        inOrder.verify(orderService).updateStatus(42L, Order.Status.PAID, null);
        inOrder.verify(loyaltyService).earnPoints(42L);
        inOrder.verify(cartRepo).save(cart);
        assertThat(cart.getStatus()).isFalse(); // giỏ được đóng
    }

    @Test
    void confirmOrder_alreadyPaid_isIdempotent_noSecondDeduction() {
        Order order = orderWithItems(42L, Order.Status.PAID);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        OrderResponse existing = givenGetByIdReturns(42L, Order.Status.PAID);

        OrderResponse result = checkoutService.confirmOrder(42L);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(productInventoryService, toppingInventoryService, loyaltyService, cartRepo);
        verify(orderService, never()).updateStatus(anyLong(), any(), any());
    }

    @ParameterizedTest(name = "confirmOrder bị từ chối khi đơn đang {0}")
    @EnumSource(value = Order.Status.class, names = { "PREPARING", "READY", "COMPLETED", "CANCELLED" })
    void confirmOrder_nonConfirmableStatus_throwsIllegalState(Order.Status from) {
        when(orderRepo.findById(42L)).thenReturn(Optional.of(orderWithItems(42L, from)));

        assertThatThrownBy(() -> checkoutService.confirmOrder(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Đơn không ở trạng thái có thể xác nhận thanh toán");

        verifyNoInteractions(productInventoryService, toppingInventoryService, loyaltyService, cartRepo);
        verify(orderService, never()).updateStatus(anyLong(), any(), any());
    }

    @Test
    void confirmOrder_orderNotFound_throwsIllegalArgument() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.confirmOrder(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy đơn 99");

        verifyNoInteractions(productInventoryService, toppingInventoryService, loyaltyService);
    }

    @Test
    void confirmOrder_itemsWithoutToppings_onlyDeductsProducts() {
        Order order = new Order();
        order.setId(42L);
        order.setCustomer(customer);
        order.setStatus(Order.Status.PENDING);
        OrderItem oi = new OrderItem();
        oi.setProduct(tea);
        oi.setQuantity(3);
        order.getItems().add(oi);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        when(orderService.updateStatus(42L, Order.Status.PAID, null))
                .thenReturn(OrderResponse.builder().id(42L).customerId(1L).status(Order.Status.PAID).build());

        checkoutService.confirmOrder(42L);

        verify(productInventoryService).deductStock(2L, order, 3);
        verifyNoInteractions(toppingInventoryService);
    }

    @Test
    void confirmOrder_multipleItems_deductsEachProductAndTopping() {
        Order order = orderWithItems(42L, Order.Status.PENDING);
        OrderItem second = new OrderItem();
        second.setProduct(tea);
        second.setQuantity(1);
        OrderItemTopping jellyTopping = new OrderItemTopping();
        jellyTopping.setTopping(jelly);
        jellyTopping.setQuantity(1);
        second.getToppings().add(jellyTopping);
        order.getItems().add(second);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        when(orderService.updateStatus(42L, Order.Status.PAID, null))
                .thenReturn(OrderResponse.builder().id(42L).customerId(1L).status(Order.Status.PAID).build());

        checkoutService.confirmOrder(42L);

        verify(productInventoryService).deductStock(1L, order, 2);
        verify(productInventoryService).deductStock(2L, order, 1);
        verify(toppingInventoryService).deductStock(7L, order, 2);
        verify(toppingInventoryService).deductStock(8L, order, 1);
    }

    @Test
    void confirmOrder_stockDeductionFails_stopsBeforeLoyaltyAndStatusChange() {
        Order order = orderWithItems(42L, Order.Status.PENDING);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        doThrow(new IllegalStateException("Không đủ hàng"))
                .when(productInventoryService).deductStock(1L, order, 2);

        assertThatThrownBy(() -> checkoutService.confirmOrder(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Không đủ hàng");

        verifyNoInteractions(loyaltyService, cartRepo);
        verify(orderService, never()).updateStatus(anyLong(), any(), any());
    }

    @Test
    void confirmOrder_redeemPointsFails_orderIsNotMarkedPaid() {
        Order order = orderWithItems(42L, Order.Status.PENDING);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        when(loyaltyService.redeemPoints(42L))
                .thenThrow(new IllegalStateException("Đơn hàng này đã trừ điểm quy đổi"));

        assertThatThrownBy(() -> checkoutService.confirmOrder(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Đơn hàng này đã trừ điểm quy đổi");

        verify(orderService, never()).updateStatus(anyLong(), any(), any());
        verify(loyaltyService, never()).earnPoints(anyLong());
        verifyNoInteractions(cartRepo);
    }

    @Test
    void confirmOrder_customerHasNoCart_doesNotFail_andSavesNothing() {
        Order order = orderWithItems(42L, Order.Status.PENDING);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        OrderResponse paid = OrderResponse.builder().id(42L).customerId(1L).status(Order.Status.PAID).build();
        when(orderService.updateStatus(42L, Order.Status.PAID, null)).thenReturn(paid);
        when(customerRepo.findById(1L)).thenReturn(Optional.empty());
        when(customerRepo.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatCode(() -> checkoutService.confirmOrder(42L)).doesNotThrowAnyException();

        verify(cartRepo, never()).save(any(Cart.class));
    }
}
