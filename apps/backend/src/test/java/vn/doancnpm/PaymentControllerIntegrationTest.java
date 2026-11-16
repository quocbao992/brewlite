package vn.doancnpm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.Order;
// Import đầy đủ các Entity và Repository
import vn.doancnpm.domain.User;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.OrderRepository;
import vn.doancnpm.repository.UserRepository;

@SpringBootTest
public class PaymentControllerIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        // Khai báo trọn bộ Repository để lưu chuỗi dữ liệu User -> Customer -> Order
        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CustomerRepository customerRepository;

        @Autowired
        private OrderRepository orderRepository;

        private MockMvc mockMvc;

        @BeforeEach
        public void setUp() {
                this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

        @Test
        public void testRequestPayment_IntegrationFlow() throws Exception {
                // Dùng UUID để test chạy nhiều lần không bị trùng lặp cột unique (email,
                // username, phone, orderCode)
                String randomStr = UUID.randomUUID().toString().substring(0, 8);

                // 1. TẠO USER TRƯỚC (Điền đủ các trường nullable = false)
                User user = new User();
                user.setUsername("testuser_" + randomStr);
                user.setFullName("Test User");
                user.setEmail("test_" + randomStr + "@gmail.com");
                user.setPasswordHash("hashed_password");
                user.setRole("ROLE_USER");
                user.setStatus(true);
                user = userRepository.save(user);

                // 2. TẠO CUSTOMER (Map với User vừa tạo)
                Customer customer = new Customer();
                customer.setUser(user);
                // Phone chỉ được 10 ký tự và unique
                customer.setPhone("09" + randomStr);
                customer = customerRepository.save(customer);

                // 3. TẠO ORDER (Map với Customer vừa tạo và điền đủ trường nullable = false)
                Order testOrder = new Order();
                testOrder.setCustomer(customer);
                testOrder.setStatus(Order.Status.PENDING);
                testOrder.setSubtotal(new BigDecimal("100000"));
                testOrder.setDiscountAmount(BigDecimal.ZERO);
                testOrder.setLoyaltyPointsUsed(0);
                testOrder.setTotalAmount(new BigDecimal("100000"));
                testOrder.setOrderCode("ORD-" + randomStr);
                testOrder.setOrderType(Order.OrderType.DINE_IN);

                testOrder = orderRepository.save(testOrder);
                Long validOrderId = testOrder.getId();

                // 4. TRUYỀN ID THẬT VÀO PAYLOAD ĐỂ GỌI API
                String requestPayload = String.format("""
                                {
                                    "orderId": %d,
                                    "method": "VNPay",
                                    "idempotencyKey": "int-test-uuid-%s"
                                }
                                """, validOrderId, randomStr);

                // 5. THỰC THI REQUEST VÀ KIỂM TRA
                mockMvc.perform(post("/api/v1/payments/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestPayload))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.orderId").value(validOrderId));
        }
}