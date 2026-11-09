package vn.doancnpm.service.implement;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
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
    // mới
    // private final ProductRepository productRepo;cũ
    // private final InventoryLogRepository inventoryLogRepo;cũ
    private final ProductInventoryService productInventoryService;
    private final ToppingInventoryService toppingInventoryService;
    private final LoyaltyService loyaltyService;
    private final PromotionService promotionService;

    // Bước 1: chuyển giỏ hàng thành đơn PENDING (chưa trừ kho, chưa đóng giỏ)
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

        // Gộp số lượng theo sản phẩm (cùng món nhưng khác size/topping vẫn tính chung 1
        // kho)
        /*
         * Map<Long, Integer> need = new HashMap<>();
         * for (CartItem ci : cartItems) {
         * need.merge(ci.getProduct().getId(), ci.getQuantity(), (a, b) -> a + b);
         * }
         * for (Map.Entry<Long, Integer> e : need.entrySet()) {
         * Product p = productRepo.findById(e.getKey())
         * .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm " +
         * e.getKey()));
         * int stock = p.getStock() == null ? 0 : p.getStock();
         * if (stock < e.getValue()) {
         * throw new IllegalStateException("Không đủ hàng: " + p.getName()
         * + " (còn " + stock + ", cần " + e.getValue() + ")");
         * }
         * }
         */

        // Gộp số lượng cần theo sản phẩm và theo topping
        // mới
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
        //
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
            oi.setProductName(ci.getProduct().getName()); // sao chép tên lúc đặt
            oi.setProductImage(ci.getProduct().getImgURL()); // sao chép ảnh lúc đặt
            oi.setSize(ci.getSize().name());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getUnitPrice()); // sao chép giá lúc đặt

            // topping của 1 ly
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

            // lineTotal = (giá món + topping của 1 ly) x số lượng
            oi.setLineTotal(ci.getUnitPrice().add(toppingPerUnit)
                    .multiply(BigDecimal.valueOf(ci.getQuantity())));
            // (Giá món * số lượng ly) + Tổng tiền topping cố định
            // BigDecimal baseTotal =
            // ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            // oi.setLineTotal(baseTotal.add(toppingPerUnit));

            order.getItems().add(oi);
            subtotal = subtotal.add(oi.getLineTotal());

        }

        order.setSubtotal(subtotal);

        // 1. Mã khuyến mãi
        String code = rq.getVoucherCode();
        BigDecimal promoDiscount = BigDecimal.ZERO;
        if (code != null && !code.isBlank()) {
            promoDiscount = promotionService.calculateDiscount(code, subtotal);
        }
        BigDecimal afterPromo = subtotal.subtract(promoDiscount);

        // 2. Điểm quy đổi
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
            promotionService.markUsed(code); // tăng lượt dùng mã
        }

        saved.setOrderCode("BL"
                + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%05d", saved.getId()));
        return orderService.getById(saved.getId());
    }

    // Bước 2: gọi sau khi thanh toán thành công -> đơn chuyển sang PAID, đóng giỏ
    @Override
    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn " + orderId));

        if (order.getStatus() == Order.Status.PAID) {
            return orderService.getById(orderId); // đã xác nhận rồi: không trừ kho/điểm lần nữa
        }
        if (order.getStatus() != Order.Status.PENDING
                && order.getStatus() != Order.Status.PAYMENT_FAILED) {
            throw new IllegalStateException("Đơn không ở trạng thái có thể xác nhận thanh toán");
        }

        /*
         * for (OrderItem oi : order.getItems()) {
         * Product p = productRepo.findById(oi.getProduct().getId())
         * .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
         * 
         * int stock = p.getStock() == null ? 0 : p.getStock();
         * if (stock < oi.getQuantity()) {
         * throw new IllegalStateException("Không đủ hàng: " + oi.getProductName());
         * }
         * 
         * p.setStock(stock - oi.getQuantity());
         * productRepo.saveAndFlush(p);
         * 
         * 
         * // 2. Ghi nhật ký vào inventory_log
         * InventoryLog log = new InventoryLog();
         * log.setChangeType("EXPORT");
         * log.setQuantityChange(-oi.getQuantity()); // Số lượng âm thể hiện xuất bán
         * log.setOrder(order);
         * log.setProduct(oi.getProduct());
         * log.setCreatedAt(LocalDateTime.now());
         * inventoryLogRepo.save(log);
         * } cũ
         */
        /*
         * for (OrderItem oi : order.getItems()) {
         * // 1. Trừ tồn kho
         * int updated = productRepo.deductStock(oi.getProduct().getId(),
         * oi.getQuantity());
         * if (updated == 0) {
         * throw new IllegalStateException("Không đủ hàng: " + oi.getProductName());
         * }
         * 
         * // 2. Ghi nhật ký vào inventory_log
         * InventoryLog log = new InventoryLog();
         * log.setChangeType("EXPORT");
         * log.setQuantityChange(-oi.getQuantity()); // Số lượng âm thể hiện xuất bán
         * log.setOrder(order);
         * log.setProduct(oi.getProduct());
         * log.setCreatedAt(LocalDateTime.now());
         * inventoryLogRepo.save(log);
         * } cũ
         */
        // mới
        for (OrderItem oi : order.getItems()) {
            productInventoryService.deductStock(oi.getProduct().getId(), order, oi.getQuantity());
            if (oi.getToppings() != null) {
                for (OrderItemTopping ot : oi.getToppings()) {
                    toppingInventoryService.deductStock(ot.getTopping().getId(), order, ot.getQuantity());
                }
            }
        } //

        loyaltyService.redeemPoints(orderId);
        OrderResponse res = orderService.updateStatus(orderId, Order.Status.PAID, null);
        loyaltyService.earnPoints(orderId); // Tích điểm cho khách khi mua hàng thành công
        closeCart(res.getCustomerId());
        return res;
    }

    // ===================== Hàm phụ =====================
    // Giỏ đang dùng của khách (status = true), không có thì trả null

    private Cart findActiveCart(Long customerId) {
        Customer customer = customerRepo.findById(customerId)
                .or(() -> customerRepo.findByUserId(customerId))
                .orElse(null);
        if (customer == null)
            return null;

        Cart cart = cartRepo.findByCustomerId(customer.getId()).orElse(null);
        if (cart != null && Boolean.FALSE.equals(cart.getStatus())) {
            cart.setStatus(true);
            cart = cartRepo.save(cart);
        }
        return cart;
    }

    // Số topping cần kiểm tra/trừ kho cho một dòng giỏ hàng.
    // Đang theo cách tính tiền hiện tại: quantity là TỔNG cho cả dòng.
    // Nếu nhóm chốt quantity là "mỗi ly" thì đổi thành: ct.getQuantity() *
    // ci.getQuantity()
    // mới
    private int toppingAmount(CartItem ci, CartItemTopping ct) {
        return ct.getQuantity();
    }//

    // Đánh dấu giỏ đã checkout (status = false)
    private void closeCart(Long customerId) {
        Cart cart = findActiveCart(customerId);
        if (cart != null) {
            cart.setStatus(false);
            cartRepo.save(cart);
        }
    }
}