package vn.doancnpm.service.implement;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.doancnpm.domain.Cart;
import vn.doancnpm.domain.CartItem;
import vn.doancnpm.domain.CartItemTopping;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.OrderItem;
import vn.doancnpm.domain.OrderItemTopping;
import vn.doancnpm.dto.Request.CheckoutRequest;
import vn.doancnpm.dto.Response.OrderResponse;
import vn.doancnpm.repository.CartItemRepository;
import vn.doancnpm.repository.CartRepository;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.service.CheckoutService;
import vn.doancnpm.service.LoyaltyService;
import vn.doancnpm.service.OrderService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.service.PromotionService;
import vn.doancnpm.service.ToppingInventoryService;

@Service
@RequiredArgsConstructor
@Transactional
public class ICheckoutService implements CheckoutService {

    private final CustomerRepository customerRepo;
    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final OrderRepository orderRepo;
    private final OrderService orderService;
    private final ProductInventoryService productInventoryService;
    private final ToppingInventoryService toppingInventoryService;
    private final LoyaltyService loyaltyService;
    private final PromotionService promotionService;

    @Override
    public OrderResponse checkout(CheckoutRequest rq) {
        Long customerId = rq.getCustomerId();
        Customer customer = customerRepo.findById(customerId)
                .or(() -> customerRepo.findByUserId(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin khách hàng"));
        Cart cart = findActiveCart(customer.getId());

        if (cart == null) {
            throw new IllegalStateException("Không có giỏ hàng đang dùng");
        }
        List<CartItem> cartItems = cartItemRepo.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }

        Map<Long, Integer> needProduct = new HashMap<>();
        Map<Long, Integer> needTopping = new HashMap<>();
        for (CartItem ci : cartItems) {
            needProduct.merge(ci.getProduct().getId(), ci.getQuantity(), Integer::sum);
            if (ci.getToppings() != null) {
                for (CartItemTopping ct : ci.getToppings()) {
                    needTopping.merge(ct.getTopping().getId(), toppingAmount(ci, ct), Integer::sum);
                }
            }
        }
        for (Map.Entry<Long, Integer> e : needProduct.entrySet()) {
            if (!productInventoryService.checkStockAvailable(e.getKey(), e.getValue())) {
                throw new IllegalStateException("Sản phẩm ID " + e.getKey()
                        + " không đủ hàng hoặc đã ngừng bán (cần " + e.getValue() + ")");
            }
        }
        for (Map.Entry<Long, Integer> e : needTopping.entrySet()) {
            if (!toppingInventoryService.checkStockAvailable(e.getKey(), e.getValue())) {
                throw new IllegalStateException("Topping ID " + e.getKey()
                        + " không đủ hàng hoặc đã ngừng bán (cần " + e.getValue() + ")");
            }
        }

        Order order = new Order();
        order.setCustomer(cart.getCustomer());
        order.setStatus(Order.Status.PENDING);
        order.setOrderType(rq.getOrderType());
        order.setTableNumber(rq.getTableNumber());
        order.setNote(rq.getNote());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setProductName(ci.getProduct().getName());
            oi.setProductImage(ci.getProduct().getImgURL());
            oi.setSize(ci.getSize().name());
            oi.setQuantity(ci.getQuantity());

            BigDecimal itemPrice = ci.getUnitPrice();
            if (itemPrice == null) {
                itemPrice = ci.getProduct() != null && ci.getProduct().getUnitPrice() != null
                        ? ci.getProduct().getUnitPrice()
                        : BigDecimal.ZERO;
            }
            oi.setUnitPrice(itemPrice);

            BigDecimal toppingPerUnit = BigDecimal.ZERO;
            if (ci.getToppings() != null) {
                for (CartItemTopping ct : ci.getToppings()) {
                    OrderItemTopping ot = new OrderItemTopping();
                    ot.setOrderItem(oi);
                    ot.setTopping(ct.getTopping());
                    ot.setToppingName(ct.getTopping().getName());
                    ot.setQuantity(ct.getQuantity());
                    ot.setUnitPrice(ct.getPrice());
                    oi.getToppings().add(ot);

                    toppingPerUnit = toppingPerUnit.add(
                            ct.getPrice().multiply(BigDecimal.valueOf(ct.getQuantity())));
                }
            }

            oi.setLineTotal(itemPrice.add(toppingPerUnit)
                    .multiply(BigDecimal.valueOf(ci.getQuantity())));

            order.getItems().add(oi);
            subtotal = subtotal.add(oi.getLineTotal());
        }

        order.setSubtotal(subtotal);

        String code = rq.getVoucherCode();
        BigDecimal promoDiscount = BigDecimal.ZERO;
        if (code != null && !code.isBlank()) {
            promoDiscount = promotionService.calculateDiscount(code, subtotal);
        }
        BigDecimal afterPromo = subtotal.subtract(promoDiscount);

        int points = rq.getLoyaltyPointsUsed() == null ? 0 : rq.getLoyaltyPointsUsed();
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (points > 0) {
            loyaltyService.validateRedeem(customerId, points);
            pointsDiscount = loyaltyService.calculateDiscountFromPoints(points);
            if (pointsDiscount.compareTo(afterPromo) > 0) {
                throw new IllegalStateException("Số điểm quy đổi vượt quá giá trị đơn hàng");
            }
        }

        order.setLoyaltyPointsUsed(points);
        order.setDiscountAmount(promoDiscount.add(pointsDiscount));
        order.setTotalAmount(subtotal.subtract(order.getDiscountAmount()));

        Order saved = orderRepo.save(order);
        if (code != null && !code.isBlank()) {
            promotionService.markUsed(code);
        }

        saved.setOrderCode("BL"
                + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%05d", saved.getId()));
        return orderService.getById(saved.getId());
    }

    @Override
    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn " + orderId));

        if (order.getStatus() == Order.Status.PAID) {
            return orderService.getById(orderId);
        }
        if (order.getStatus() != Order.Status.PENDING
                && order.getStatus() != Order.Status.PAYMENT_FAILED) {
            throw new IllegalStateException("Đơn không ở trạng thái có thể xác nhận thanh toán");
        }

        for (OrderItem oi : order.getItems()) {
            productInventoryService.deductStock(oi.getProduct().getId(), order, oi.getQuantity());
            if (oi.getToppings() != null) {
                for (OrderItemTopping ot : oi.getToppings()) {
                    toppingInventoryService.deductStock(ot.getTopping().getId(), order, ot.getQuantity());
                }
            }
        }

        loyaltyService.redeemPoints(orderId);
        OrderResponse res = orderService.updateStatus(orderId, Order.Status.PAID, null);
        loyaltyService.earnPoints(orderId);
        closeCart(res.getCustomerId());
        return res;
    }

    private Cart findActiveCart(Long customerId) {
        Customer customer = customerRepo.findById(customerId)
                .or(() -> customerRepo.findByUserId(customerId))
                .orElse(null);
        if (customer == null) {
            return null;
        }

        Cart cart = cartRepo.findByCustomerId(customer.getId()).orElse(null);
        if (cart != null && Boolean.FALSE.equals(cart.getStatus())) {
            cart.setStatus(true);
            cart = cartRepo.save(cart);
        }
        return cart;
    }

    private int toppingAmount(CartItem ci, CartItemTopping ct) {
        return ct.getQuantity();
    }

    private void closeCart(Long customerId) {
        Cart cart = findActiveCart(customerId);
        if (cart != null) {
            cart.setStatus(false);
            cartRepo.save(cart);
        }
    }
}
