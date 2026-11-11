package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.doancnpm.config.GlobalExceptionHandler;
import vn.doancnpm.domain.Order;
import vn.doancnpm.dto.Request.CheckoutRequest;
import vn.doancnpm.dto.Response.OrderResponse;
import vn.doancnpm.dto.Response.OrderSummaryResponse;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.service.CheckoutService;
import vn.doancnpm.service.OrderService;
import vn.doancnpm.controller.OrderController;


@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final String BASE = "/api/v1/orders";
    private static final String DEFAULT_CANCEL_REASON = "Khách hàng yêu cầu hủy qua ứng dụng";

    @Mock
    private OrderService orderService;

    @Mock
    private CheckoutService checkoutService;

    @InjectMocks
    private OrderController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- helpers ----------

    private static OrderResponse orderResponse(long id, Order.Status st) {
        return OrderResponse.builder()
                .id(id)
                .customerId(1L)
                .orderCode("BL2026110900" + id)
                .status(st)
                .subtotal(new BigDecimal("90000"))
                .discountAmount(new BigDecimal("0"))
                .totalAmount(new BigDecimal("90000"))
                .build();
    }

    /** Đọc body bằng UTF-8 để assert được cả tiếng Việt. */
    private static ResultMatcher bodyContains(String text) {
        return result -> assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains(text);
    }

    // ===================== POST /checkout =====================

    private static final String VALID_CHECKOUT = """
            {"customerId":1,"orderType":"DINE_IN","tableNumber":"A1",
             "voucherCode":"SALE10","loyaltyPointsUsed":5,"note":"it da"}
            """;

    @Test
    void checkout_validRequest_returns200AndBody() throws Exception {
        when(checkoutService.checkout(any(CheckoutRequest.class)))
                .thenReturn(orderResponse(42, Order.Status.PENDING));

        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CHECKOUT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(90000));
    }

    @Test
    void checkout_bindsAllFieldsIntoCheckoutRequest() throws Exception {
        when(checkoutService.checkout(any(CheckoutRequest.class)))
                .thenReturn(orderResponse(1, Order.Status.PENDING));

        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CHECKOUT))
                .andExpect(status().isOk());

        ArgumentCaptor<CheckoutRequest> captor = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(checkoutService).checkout(captor.capture());
        CheckoutRequest rq = captor.getValue();
        assertThat(rq.getCustomerId()).isEqualTo(1L);
        assertThat(rq.getOrderType()).isEqualTo(Order.OrderType.DINE_IN);
        assertThat(rq.getTableNumber()).isEqualTo("A1");
        assertThat(rq.getVoucherCode()).isEqualTo("SALE10");
        assertThat(rq.getLoyaltyPointsUsed()).isEqualTo(5);
        assertThat(rq.getNote()).isEqualTo("it da");
    }

    @Test
    void checkout_onlyRequiredFields_optionalFieldsAreNull() throws Exception {
        when(checkoutService.checkout(any(CheckoutRequest.class)))
                .thenReturn(orderResponse(1, Order.Status.PENDING));

        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":1,\"orderType\":\"TAKE_AWAY\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CheckoutRequest> captor = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(checkoutService).checkout(captor.capture());
        CheckoutRequest rq = captor.getValue();
        assertThat(rq.getOrderType()).isEqualTo(Order.OrderType.TAKE_AWAY);
        assertThat(rq.getTableNumber()).isNull();
        assertThat(rq.getVoucherCode()).isNull();
        assertThat(rq.getLoyaltyPointsUsed()).isNull();
        assertThat(rq.getNote()).isNull();
    }

    @Test
    void checkout_boundaryValues_zeroPointsAndNote500Chars_areValid() throws Exception {
        when(checkoutService.checkout(any(CheckoutRequest.class)))
                .thenReturn(orderResponse(1, Order.Status.PENDING));
        String note500 = "x".repeat(500);

        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":1,\"orderType\":\"DINE_IN\",\"loyaltyPointsUsed\":0,\"note\":\""
                        + note500 + "\"}"))
                .andExpect(status().isOk());
    }

    static Stream<Arguments> invalidCheckoutBodies() {
        return Stream.of(
                Arguments.of("thiếu customerId", "{\"orderType\":\"DINE_IN\"}"),
                Arguments.of("customerId = null", "{\"customerId\":null,\"orderType\":\"DINE_IN\"}"),
                Arguments.of("thiếu orderType", "{\"customerId\":1}"),
                Arguments.of("loyaltyPointsUsed âm",
                        "{\"customerId\":1,\"orderType\":\"DINE_IN\",\"loyaltyPointsUsed\":-1}"),
                Arguments.of("note dài 501 ký tự",
                        "{\"customerId\":1,\"orderType\":\"DINE_IN\",\"note\":\"" + "x".repeat(501) + "\"}"),
                Arguments.of("orderType không tồn tại", "{\"customerId\":1,\"orderType\":\"DELIVERY\"}"),
                Arguments.of("customerId không phải số", "{\"customerId\":\"abc\",\"orderType\":\"DINE_IN\"}"),
                Arguments.of("JSON hỏng", "{\"customerId\":1,"),
                Arguments.of("body rỗng", ""));
    }

    @ParameterizedTest(name = "checkout 400: {0}")
    @MethodSource("invalidCheckoutBodies")
    void checkout_invalidBody_returns400_andNeverCallsService(String ignoredCase, String body) throws Exception {
        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(checkoutService);
    }

    @Test
    void checkout_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.TEXT_PLAIN)
                .content(VALID_CHECKOUT))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(checkoutService);
    }

    @Test
    void checkout_serviceThrowsIllegalState_returns400WithMessage() throws Exception {
        when(checkoutService.checkout(any(CheckoutRequest.class)))
                .thenThrow(new IllegalStateException("Giỏ hàng trống"));

        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CHECKOUT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(bodyContains("Giỏ hàng trống"));
    }

    @Test
    void checkout_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(checkoutService.checkout(any(CheckoutRequest.class)))
                .thenThrow(new IllegalArgumentException("customer not found"));

        mockMvc.perform(post(BASE + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CHECKOUT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("customer not found"));
    }

    // ===================== GET / (lịch sử) =====================

    @Test
    void history_defaultPaging_callsServiceWithPage0Size10() throws Exception {
        PageResponse<OrderSummaryResponse> page = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(orderService.getHistory(1L, null, 0, 10)).thenReturn(page);

        mockMvc.perform(get(BASE).param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(orderService).getHistory(1L, null, 0, 10);
    }

    @Test
    void history_withStatusAndPaging_passedToService() throws Exception {
        OrderSummaryResponse summary = OrderSummaryResponse.builder()
                .id(5L).orderCode("BL202611090005").status(Order.Status.COMPLETED)
                .totalAmount(new BigDecimal("120000")).totalItems(3).build();
        PageResponse<OrderSummaryResponse> page = new PageResponse<>(List.of(summary), 1, 5, 6, 2);
        when(orderService.getHistory(7L, Order.Status.COMPLETED, 1, 5)).thenReturn(page);

        mockMvc.perform(get(BASE)
                .param("customerId", "7")
                .param("status", "COMPLETED")
                .param("page", "1")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderCode").value("BL202611090005"))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].totalItems").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void history_missingCustomerId_returns400() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void history_nonNumericCustomerId_returns400() throws Exception {
        mockMvc.perform(get(BASE).param("customerId", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void history_unknownStatus_returns400() throws Exception {
        mockMvc.perform(get(BASE).param("customerId", "1").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void history_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(orderService.getHistory(1L, null, -1, 10))
                .thenThrow(new IllegalArgumentException("Page index must not be less than zero"));

        mockMvc.perform(get(BASE).param("customerId", "1").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ===================== GET /queue =====================

    @Test
    void queue_noStatus_defaultsToPaidPreparingReady() throws Exception {
        List<Order.Status> defaults = List.of(Order.Status.PAID, Order.Status.PREPARING, Order.Status.READY);
        when(orderService.getQueue(defaults)).thenReturn(List.of(
                orderResponse(1, Order.Status.PAID),
                orderResponse(2, Order.Status.PREPARING)));

        mockMvc.perform(get(BASE + "/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[1].status").value("PREPARING"));

        verify(orderService).getQueue(defaults);
    }

    @Test
    void queue_singleStatus_usesGivenStatus() throws Exception {
        List<Order.Status> only = List.of(Order.Status.READY);
        when(orderService.getQueue(only)).thenReturn(List.of(orderResponse(3, Order.Status.READY)));

        mockMvc.perform(get(BASE + "/queue").param("status", "READY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("READY"));

        verify(orderService).getQueue(only);
    }

    @Test
    void queue_repeatedStatusParams_areCollectedIntoList() throws Exception {
        List<Order.Status> expected = List.of(Order.Status.PAID, Order.Status.READY);
        when(orderService.getQueue(expected)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/queue").param("status", "PAID", "READY"))
                .andExpect(status().isOk());

        verify(orderService).getQueue(expected);
    }

    @Test
    void queue_commaSeparatedStatus_areCollectedIntoList() throws Exception {
        List<Order.Status> expected = List.of(Order.Status.PAID, Order.Status.PREPARING);
        when(orderService.getQueue(expected)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/queue").param("status", "PAID,PREPARING"))
                .andExpect(status().isOk());

        verify(orderService).getQueue(expected);
    }

    @Test
    void queue_emptyResult_returnsEmptyArray() throws Exception {
        when(orderService.getQueue(any())).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void queue_unknownStatus_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/queue").param("status", "PAID", "BOGUS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    // ===================== GET /code/{orderCode} & GET /{id} =====================

    @Test
    void byCode_found_returns200() throws Exception {
        when(orderService.getByCode("BL202611090042")).thenReturn(orderResponse(42, Order.Status.PAID));

        mockMvc.perform(get(BASE + "/code/BL202611090042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(orderService).getByCode("BL202611090042");
    }

    /** Hiện tại "không tìm thấy" được service ném IllegalArgumentException -> handler map thành 400 (không phải 404). */
    @Test
    void byCode_notFound_returns400() throws Exception {
        when(orderService.getByCode("NOPE")).thenThrow(new IllegalArgumentException("order not found NOPE"));

        mockMvc.perform(get(BASE + "/code/NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("order not found NOPE"));
    }

    @Test
    void detail_found_returns200() throws Exception {
        when(orderService.getById(42L)).thenReturn(orderResponse(42, Order.Status.READY));

        mockMvc.perform(get(BASE + "/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void detail_notFound_returns400() throws Exception {
        when(orderService.getById(99L)).thenThrow(new IllegalArgumentException("order not found 99"));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("order not found 99"));
    }

    @Test
    void detail_nonNumericId_returns400_andNeverCallsService() throws Exception {
        mockMvc.perform(get(BASE + "/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    // ===================== PUT /{id}/status =====================

    @Test
    void updateStatus_toPaid_isRejectedWith400_andServiceNotCalled() throws Exception {
        mockMvc.perform(put(BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(bodyContains("PAID"));

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "updateStatus -> {0} được chuyển xuống service")
    @EnumSource(value = Order.Status.class, mode = EnumSource.Mode.EXCLUDE, names = "PAID")
    void updateStatus_anyStatusExceptPaid_delegatesToService(Order.Status target) throws Exception {
        when(orderService.updateStatus(1L, target, null)).thenReturn(orderResponse(1, target));

        mockMvc.perform(put(BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + target.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(target.name()));

        verify(orderService).updateStatus(1L, target, null);
    }

    @Test
    void updateStatus_withReason_reasonIsForwarded() throws Exception {
        when(orderService.updateStatus(1L, Order.Status.CANCELLED, "het nguyen lieu"))
                .thenReturn(orderResponse(1, Order.Status.CANCELLED));

        mockMvc.perform(put(BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\",\"reason\":\"het nguyen lieu\"}"))
                .andExpect(status().isOk());

        verify(orderService).updateStatus(1L, Order.Status.CANCELLED, "het nguyen lieu");
    }

    @Test
    void updateStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(put(BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"abc\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "updateStatus 400 với body: {0}")
    @ValueSource(strings = { "{\"status\":\"SHIPPED\"}", "{\"status\":null}", "{", "" })
    void updateStatus_invalidBody_returns400(String body) throws Exception {
        mockMvc.perform(put(BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatus_nonNumericId_returns400() throws Exception {
        mockMvc.perform(put(BASE + "/abc/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatus_invalidTransition_serviceThrowsIllegalState_returns400() throws Exception {
        when(orderService.updateStatus(1L, Order.Status.COMPLETED, null))
                .thenThrow(new IllegalStateException("invalid transition"));

        mockMvc.perform(put(BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid transition"));
    }

    @Test
    void updateStatus_orderNotFound_returns400() throws Exception {
        when(orderService.updateStatus(99L, Order.Status.PREPARING, null))
                .thenThrow(new IllegalArgumentException("order not found 99"));

        mockMvc.perform(put(BASE + "/99/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isBadRequest());
    }

    // ===================== PUT|POST /{id}/cancel =====================

    @Test
    void cancel_put_withReason_usesGivenReason() throws Exception {
        when(orderService.cancel(1L, "Khách đổi ý")).thenReturn(orderResponse(1, Order.Status.CANCELLED));

        mockMvc.perform(put(BASE + "/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content("{\"reason\":\"Khách đổi ý\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancel(1L, "Khách đổi ý");
    }

    @Test
    void cancel_post_isAlsoSupported() throws Exception {
        when(orderService.cancel(1L, "doi y")).thenReturn(orderResponse(1, Order.Status.CANCELLED));

        mockMvc.perform(post(BASE + "/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"doi y\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancel(1L, "doi y");
    }

    @Test
    void cancel_noBody_usesDefaultReason() throws Exception {
        when(orderService.cancel(1L, DEFAULT_CANCEL_REASON))
                .thenReturn(orderResponse(1, Order.Status.CANCELLED));

        mockMvc.perform(put(BASE + "/1/cancel"))
                .andExpect(status().isOk());

        verify(orderService).cancel(1L, DEFAULT_CANCEL_REASON);
    }

    @ParameterizedTest(name = "cancel dùng lý do mặc định khi body = {0}")
    @ValueSource(strings = { "{}", "{\"reason\":null}", "{\"reason\":\"\"}", "{\"reason\":\"   \"}" })
    void cancel_nullOrBlankReason_usesDefaultReason(String body) throws Exception {
        when(orderService.cancel(1L, DEFAULT_CANCEL_REASON))
                .thenReturn(orderResponse(1, Order.Status.CANCELLED));

        mockMvc.perform(post(BASE + "/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        verify(orderService).cancel(1L, DEFAULT_CANCEL_REASON);
    }

    @Test
    void cancel_alreadyPaid_serviceThrowsIllegalState_returns400() throws Exception {
        when(orderService.cancel(1L, "x")).thenThrow(new IllegalStateException("already paid"));

        mockMvc.perform(put(BASE + "/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("already paid"));
    }

    @Test
    void cancel_nonNumericId_returns400() throws Exception {
        mockMvc.perform(put(BASE + "/abc/cancel"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void cancel_getIsNotAllowed_returns405() throws Exception {
        mockMvc.perform(get(BASE + "/1/cancel"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(orderService);
    }

    // ===================== POST /{id}/confirm =====================

    @Test
    void confirm_success_returns200AndCallsCheckoutService() throws Exception {
        when(checkoutService.confirmOrder(5L)).thenReturn(orderResponse(5, Order.Status.PAID));

        mockMvc.perform(post(BASE + "/5/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(checkoutService).confirmOrder(5L);
        verifyNoInteractions(orderService);
    }

    @Test
    void confirm_invalidState_returns400() throws Exception {
        when(checkoutService.confirmOrder(5L)).thenThrow(new IllegalStateException("cannot confirm"));

        mockMvc.perform(post(BASE + "/5/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("cannot confirm"));
    }

    @Test
    void confirm_orderNotFound_returns400() throws Exception {
        when(checkoutService.confirmOrder(99L)).thenThrow(new IllegalArgumentException("order not found 99"));

        mockMvc.perform(post(BASE + "/99/confirm"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirm_nonNumericId_returns400() throws Exception {
        mockMvc.perform(post(BASE + "/abc/confirm"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(checkoutService);
    }

    @Test
    void confirm_getIsNotAllowed_returns405() throws Exception {
        mockMvc.perform(get(BASE + "/5/confirm"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(checkoutService);
    }
}
