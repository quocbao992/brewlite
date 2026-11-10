package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.doancnpm.config.GlobalExceptionHandler;
import vn.doancnpm.domain.Order;
import vn.doancnpm.dto.Response.AdminOrderResponse;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.service.OrderService;
import vn.doancnpm.controller.AdminOrderController;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    private static final String URL = "/api/admin/orders";

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminOrderController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- helpers ----------

    private static PageResponse<AdminOrderResponse> emptyPage(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0, 0);
    }

    private static AdminOrderResponse adminOrder(long id, String code, String name, String phone,
            String total, Order.Status st) {
        return AdminOrderResponse.builder()
                .id(id)
                .orderCode(code)
                .customerName(name)
                .phone(phone)
                .totalAmount(new BigDecimal(total))
                .status(st)
                .build();
    }

    private static ResultMatcher bodyContains(String text) {
        return result -> assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains(text);
    }

    // ===================== HAPPY PATH =====================

    @Test
    void search_noParams_usesDefaultPaging_andNullFilters() throws Exception {
        when(orderService.adminSearch(null, null, null, 0, 10)).thenReturn(emptyPage(0, 10));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(orderService).adminSearch(null, null, null, 0, 10);
    }

    @Test
    void search_allParams_passedToService() throws Exception {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 9);
        when(orderService.adminSearch(from, to, Order.Status.PAID, 2, 5)).thenReturn(emptyPage(2, 5));

        mockMvc.perform(get(URL)
                .param("fromDate", "2026-11-01")
                .param("toDate", "2026-11-09")
                .param("status", "PAID")
                .param("page", "2")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(orderService).adminSearch(from, to, Order.Status.PAID, 2, 5);
    }

    @Test
    void search_returnsPagedContentAsJson() throws Exception {
        PageResponse<AdminOrderResponse> page = new PageResponse<>(
                List.of(
                        adminOrder(1, "BL2026110900001", "Nguyen Van A", "0901234567", "150000", Order.Status.PAID),
                        adminOrder(2, "BL2026110900002", "Tran Thi B", "0987654321", "90000", Order.Status.COMPLETED)),
                0, 10, 2, 1);
        when(orderService.adminSearch(null, null, null, 0, 10)).thenReturn(page);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].orderCode").value("BL2026110900001"))
                .andExpect(jsonPath("$.content[0].customerName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.content[0].phone").value("0901234567"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(150000))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.content[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void search_onlyFromDate_toDateAndStatusStayNull() throws Exception {
        LocalDate from = LocalDate.of(2026, 11, 1);
        when(orderService.adminSearch(from, null, null, 0, 10)).thenReturn(emptyPage(0, 10));

        mockMvc.perform(get(URL).param("fromDate", "2026-11-01"))
                .andExpect(status().isOk());

        verify(orderService).adminSearch(from, null, null, 0, 10);
    }

    @Test
    void search_onlyToDate_fromDateAndStatusStayNull() throws Exception {
        LocalDate to = LocalDate.of(2026, 11, 9);
        when(orderService.adminSearch(null, to, null, 0, 10)).thenReturn(emptyPage(0, 10));

        mockMvc.perform(get(URL).param("toDate", "2026-11-09"))
                .andExpect(status().isOk());

        verify(orderService).adminSearch(null, to, null, 0, 10);
    }

    @ParameterizedTest(name = "status={0} được chuyển xuống service")
    @EnumSource(Order.Status.class)
    void search_everyOrderStatus_isAccepted(Order.Status st) throws Exception {
        when(orderService.adminSearch(null, null, st, 0, 10)).thenReturn(emptyPage(0, 10));

        mockMvc.perform(get(URL).param("status", st.name()))
                .andExpect(status().isOk());

        verify(orderService).adminSearch(null, null, st, 0, 10);
    }

    @Test
    void search_sameDayRange_fromEqualsTo_isAllowed() throws Exception {
        LocalDate day = LocalDate.of(2026, 11, 9);
        when(orderService.adminSearch(day, day, null, 0, 10)).thenReturn(emptyPage(0, 10));

        mockMvc.perform(get(URL).param("fromDate", "2026-11-09").param("toDate", "2026-11-09"))
                .andExpect(status().isOk());

        verify(orderService).adminSearch(day, day, null, 0, 10);
    }

    /**
     * Ghi nhận hành vi HIỆN TẠI: controller không validate fromDate > toDate,
     * nó chỉ chuyển thẳng xuống service (kết quả sẽ rỗng). Nếu sau này nhóm thêm
     * validate (trả 400) thì cập nhật lại test này.
     */
    @Test
    void search_fromDateAfterToDate_isNotValidatedByController() throws Exception {
        LocalDate from = LocalDate.of(2026, 11, 9);
        LocalDate to = LocalDate.of(2026, 11, 1);
        when(orderService.adminSearch(from, to, null, 0, 10)).thenReturn(emptyPage(0, 10));

        mockMvc.perform(get(URL).param("fromDate", "2026-11-09").param("toDate", "2026-11-01"))
                .andExpect(status().isOk());

        verify(orderService).adminSearch(from, to, null, 0, 10);
    }

    // ===================== INPUT KHÔNG HỢP LỆ =====================

    @ParameterizedTest(name = "fromDate=''{0}'' -> 400")
    @ValueSource(strings = { "09-11-2026", "2026/11/09", "abc", "2026-13-01", "2026-02-30" })
    void search_invalidFromDate_returns400(String badDate) throws Exception {
        mockMvc.perform(get(URL).param("fromDate", badDate))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void search_invalidToDate_returns400() throws Exception {
        mockMvc.perform(get(URL).param("toDate", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void search_unknownStatus_returns400() throws Exception {
        mockMvc.perform(get(URL).param("status", "DELIVERED"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void search_statusIsCaseSensitive_lowercaseReturns400() throws Exception {
        mockMvc.perform(get(URL).param("status", "paid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "{0}=abc -> 400")
    @ValueSource(strings = { "page", "size" })
    void search_nonNumericPagingParam_returns400(String param) throws Exception {
        mockMvc.perform(get(URL).param(param, "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    // ===================== LỖI TỪ SERVICE =====================

    /** PageRequest.of(-1, ..) trong service ném IllegalArgumentException -> GlobalExceptionHandler trả 400. */
    @Test
    void search_serviceThrowsIllegalArgument_returns400WithMessage() throws Exception {
        when(orderService.adminSearch(null, null, null, -1, 10))
                .thenThrow(new IllegalArgumentException("Page index must not be less than zero"));

        mockMvc.perform(get(URL).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }

    @Test
    void search_serviceThrowsIllegalState_returns400() throws Exception {
        when(orderService.adminSearch(null, null, null, 0, 10))
                .thenThrow(new IllegalStateException("bad state"));

        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(bodyContains("bad state"));
    }

    // ===================== HTTP METHOD / URL =====================

    @Test
    void search_postIsNotAllowed_returns405() throws Exception {
        mockMvc.perform(post(URL))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(orderService);
    }
}
