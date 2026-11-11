package vn.doancnpm;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.doancnpm.controller.MockGatewayController;

class MockGatewayControllerTest {

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockGatewayController controller = new MockGatewayController();
        // trì hoãn rất lâu để test KHÔNG thật sự gọi webhook
        ReflectionTestUtils.setField(controller, "delayMs", 600000L);
        ReflectionTestUtils.setField(controller, "successRate", 0.8);
        ReflectionTestUtils.setField(controller, "baseUrl", "http://localhost:1");
        ReflectionTestUtils.setField(controller, "secret", "mock-secret");
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void pay_withResultSuccess_returns202AndSuccess() throws Exception {
        mvc.perform(get("/mock-gateway/pay")
                .param("orderId", "36").param("amount", "45000")
                .param("method", "VNPay").param("ref", "k1").param("result", "SUCCESS"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.orderId").value(36))
            .andExpect(jsonPath("$.simulatedResult").value("SUCCESS"))
            .andExpect(jsonPath("$.transactionId").value(org.hamcrest.Matchers.startsWith("MOCK-")));
    }

    @Test
    void pay_withResultFailed_returnsFailed() throws Exception {
        mvc.perform(get("/mock-gateway/pay")
                .param("orderId", "36").param("method", "VNPay")
                .param("ref", "k1").param("result", "failed"))      // chữ thường vẫn được
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.simulatedResult").value("FAILED"));
    }

    @Test
    void pay_withoutResult_isRandomButValid() throws Exception {
        mvc.perform(get("/mock-gateway/pay")
                .param("orderId", "36").param("method", "VNPay").param("ref", "k1"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.simulatedResult").value(anyOf(is("SUCCESS"), is("FAILED"))));
    }

    @Test
    void pay_acceptsPostToo() throws Exception {
        mvc.perform(post("/mock-gateway/pay")
                .param("orderId", "36").param("method", "VNPay")
                .param("ref", "k1").param("result", "SUCCESS"))
            .andExpect(status().isAccepted());
    }

    @Test
    void pay_missingRef_returns400() throws Exception {
        mvc.perform(get("/mock-gateway/pay")
                .param("orderId", "36").param("method", "VNPay"))
            .andExpect(status().isBadRequest());
    }
}