package vn.doancnpm.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.doancnpm.domain.Category;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.InventoryProduct;
import vn.doancnpm.domain.InventoryTopping;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.OrderItem;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.domain.User;
import vn.doancnpm.repository.CategoryRepository;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.InventoryProductRepository;
import vn.doancnpm.repository.InventoryToppingRepository;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InventoryProductRepository inventoryProductRepository;
    private final InventoryToppingRepository inventoryToppingRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Kiem tra va khoi tao du lieu mau he thong (DataSeeder)...");
        seedUsersAndCustomers();
        seedCategories();
        seedToppings();
        seedProducts();
        seedOrders();
        log.info("DataSeeder hoan tat thanh cong.");
    }

    private void seedUsersAndCustomers() {
        seedUser("admin", "Admin BrewLite", "admin@brewlite.vn", "Admin@123", "ADMIN", null);
        seedUser("barista01", "Nguyen Van An", "barista01@brewlite.vn", "Barista@123", "BARISTA", "0901111001");
        seedUser("cashier01", "Tran Thi Binh", "cashier01@brewlite.vn", "Cashier@123", "CASHIER", "0901111002");
        seedUser("customer01", "Pham Quoc Bao", "customer01@brewlite.vn", "Customer@123", "CUSTOMER", "0901111003");
        seedUser("customer02", "Le Thi Mai", "customer02@brewlite.vn", "Customer@123", "CUSTOMER", "0901111004");
        seedUser("customer03", "Nguyen Hoang Nam", "customer03@brewlite.vn", "Customer@123", "CUSTOMER", "0901111005");
    }

    private User seedUser(String username, String fullName, String email,
            String rawPassword, String role, String phone) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setStatus(true);
            User saved = userRepository.save(user);

            if ("CUSTOMER".equals(role)) {
                Customer customer = Customer.builder()
                        .user(saved)
                        .phone(phone)
                        .gender(true)
                        .date(LocalDate.of(2000, 1, 1))
                        .loyaltyP(120)
                        .build();
                customerRepository.save(customer);
            }
            log.info("Da tao tai khoan [{}] username='{}'", role, username);
            return saved;
        });
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository
                    .save(Category.builder().name("Ca Phe Truyen Thong").displayOrder(1L).status(true).build());
            categoryRepository
                    .save(Category.builder().name("Espresso & Ca Phe Y").displayOrder(2L).status(true).build());
            categoryRepository
                    .save(Category.builder().name("Cold Brew Dac Biet").displayOrder(3L).status(true).build());
            categoryRepository.save(Category.builder().name("Tra Trai Cay").displayOrder(4L).status(true).build());
            categoryRepository.save(Category.builder().name("Banh Ngot An Kem").displayOrder(5L).status(true).build());
            log.info("Da khoi tao danh muc.");
        }
    }

    private void seedToppings() {
        if (toppingRepository.count() == 0) {
            seedTopping("Tran Chau Trang", 10000, 200);
            seedTopping("Thach Ca Phe", 10000, 200);
            seedTopping("Kem Cheese Beo", 15000, 200);
            seedTopping("Sot Caramel", 8000, 200);
            log.info("Da khoi tao toppings.");
        }
    }

    private void seedTopping(String name, long price, int stock) {
        Topping topping = toppingRepository.save(Topping.builder()
                .name(name)
                .unitPrice(BigDecimal.valueOf(price))
                .status(true)
                .build());
        inventoryToppingRepository.save(InventoryTopping.builder()
                .topping(topping)
                .stock(stock)
                .minThreshold(10)
                .build());
    }

    private Product seedProduct(String name, Category category, long price, int stock, String image) {
        Product product = productRepository.save(Product.builder()
                .name(name)
                .category(category)
                .unitPrice(BigDecimal.valueOf(price))
                .status(true)
                .imgURL(image)
                .build());
        inventoryProductRepository.save(InventoryProduct.builder()
                .product(product)
                .stock(stock)
                .minThreshold(5)
                .build());
        return product;
    }

    private void seedProductIfMissing(String name, Category category, long price, int stock, String image) {
        if (!productRepository.existsByNameIgnoreCase(name)) {
            seedProduct(name, category, price, stock, image);
        }
    }

    private void seedProducts() {
        List<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        if (categories.isEmpty())
            return;

        Category catTrad = categories.get(0);
        Category catMachine = categories.size() > 1 ? categories.get(1) : catTrad;
        Category catColdBrew = categories.size() > 2 ? categories.get(2) : catTrad;
        Category catTea = categories.size() > 3 ? categories.get(3) : catTrad;
        Category catPastry = categories.size() > 4 ? categories.get(4) : catTrad;

        seedProductIfMissing("Ca Phe Sua Da Sai Gon", catTrad, 29000, 100, "cf-sua-da.jpg");
        seedProductIfMissing("Ca Phe Den Da Dam Vi", catTrad, 25000, 100, "cf-den-da.jpg");
        seedProductIfMissing("Bac Xiu Kem Sua 3 Tang", catTrad, 35000, 100, "bac-xiu.jpg");
        seedProductIfMissing("Caffe Latte Thom Beo", catMachine, 45000, 80, "caffe-latte.jpg");
        seedProductIfMissing("Caramel Macchiato", catMachine, 49000, 80, "caramel-macchiato.jpg");
        seedProductIfMissing("Cold Brew Cam Sa Mat Lanh", catColdBrew, 48000, 60, "cold-brew.jpg");
        seedProductIfMissing("Tra Dao Cam Sa Tuoi Mat", catTea, 39000, 90, "tra-dao.jpg");
        seedProductIfMissing("Banh Sung Bo Croissant Bo Phap", catPastry, 32000, 50, "croissant.jpg");
        log.info("Da khoi tao san pham.");
    }

    private void seedOrders() {
        if (orderRepository.count() == 0) {
            List<Customer> customers = customerRepository.findAll();
            List<Product> products = productRepository.findAll();
            if (customers.isEmpty() || products.isEmpty())
                return;

            Customer c1 = customers.get(0);
            Customer c2 = customers.size() > 1 ? customers.get(1) : c1;
            Customer c3 = customers.size() > 2 ? customers.get(2) : c1;

            Product p1 = products.get(0);
            Product p2 = products.size() > 1 ? products.get(1) : p1;
            Product p3 = products.size() > 2 ? products.get(2) : p1;
            Product p4 = products.size() > 3 ? products.get(3) : p1;

            createOrder(c1, "ORD-20261028-001", Order.Status.PAID, Order.OrderType.DINE_IN, "Ban 02",
                    List.of(createItem(p1, "M", 2, p1.getUnitPrice()), createItem(p3, "L", 1, p3.getUnitPrice())));

            createOrder(c2, "ORD-20261028-002", Order.Status.PREPARING, Order.OrderType.TAKE_AWAY, null,
                    List.of(createItem(p4, "L", 2, p4.getUnitPrice())));

            createOrder(c3, "ORD-20261028-003", Order.Status.READY, Order.OrderType.DINE_IN, "Ban 05",
                    List.of(createItem(p2, "M", 1, p2.getUnitPrice()), createItem(p1, "L", 1, p1.getUnitPrice())));

            createOrder(c1, "ORD-20261028-004", Order.Status.COMPLETED, Order.OrderType.TAKE_AWAY, null,
                    List.of(createItem(p3, "M", 1, p3.getUnitPrice())));

            createOrder(c2, "ORD-20261028-005", Order.Status.CANCELLED, Order.OrderType.DINE_IN, "Ban 01",
                    List.of(createItem(p4, "M", 1, p4.getUnitPrice())));

            log.info("Da khoi tao don hang mau.");
        }
    }

    private void createOrder(Customer customer, String orderCode, Order.Status status,
            Order.OrderType orderType, String tableNumber,
            List<OrderItem> items) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderCode(orderCode);
        order.setStatus(status);
        order.setOrderType(orderType);
        order.setTableNumber(tableNumber);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setLoyaltyPointsUsed(0);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            item.setOrder(order);
            subtotal = subtotal.add(item.getLineTotal());
        }
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);
        order.setItems(new ArrayList<>(items));

        orderRepository.save(order);
    }

    private OrderItem createItem(Product product, String size, int qty, BigDecimal unitPrice) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setProductImage(product.getImgURL());
        item.setSize(size);
        item.setQuantity(qty);
        item.setUnitPrice(unitPrice);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)));
        return item;
    }
}
