package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.doancnpm.controller.InventoryController;
import vn.doancnpm.dto.Response.InventoryLogResponse;
import vn.doancnpm.dto.Response.InventoryStockResponse;
import vn.doancnpm.service.InventoryLogService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.service.ToppingInventoryService;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private LocalValidatorFactoryBean validator;

  @Mock
  private ProductInventoryService productInventoryService;

  @Mock
  private ToppingInventoryService toppingInventoryService;

  @Mock
  private InventoryLogService inventoryLogService;

  @InjectMocks
  private InventoryController inventoryController;

  private static final String BASE = "/api/v1/inventory";

  @BeforeEach
  @SuppressWarnings({ "removal", "deprecation" })
  void setUp() {
    validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
        .setValidator(validator)
        .setMessageConverters(
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
        .build();
  }

  
  private void expectRejected(MockHttpServletRequestBuilder request) throws Exception {
    try {
      mockMvc.perform(request).andExpect(status().isBadRequest());
    } catch (ServletException ex) {
      assertThat(ex).hasRootCauseInstanceOf(IllegalArgumentException.class);
    }
  }

  /** Gửi POST với body JSON. */
  private ResultActions postJson(String url, String body) throws Exception {
    return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
  }

// ##########################################################
  //                         TOPPING
  // ##########################################################

  // ==========================================
  // 1. GET /api/v1/inventory/toppings/{toppingId}
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/inventory/toppings/{toppingId}")
  class GetToppingStockEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng thông tin tồn kho")
    void getToppingStock_ReturnsOk() throws Exception {
      InventoryStockResponse response = InventoryStockResponse.builder()
          .itemId(1L).itemName("Trân châu").stock(20).minThreshold(10).build();
      when(toppingInventoryService.getStock(1L)).thenReturn(response);

      mockMvc.perform(get(BASE + "/toppings/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.itemId").value(1))
          .andExpect(jsonPath("$.itemName").value("Trân châu"))
          .andExpect(jsonPath("$.stock").value(20))
          .andExpect(jsonPath("$.minThreshold").value(10));

      verify(toppingInventoryService, times(1)).getStock(1L);
    }

    @Test
    @DisplayName("Chưa có tồn kho: EntityNotFoundException bị ném ra ngoài (không có handler)")
    void getToppingStock_NotFound_Returns404() throws Exception {
      when(toppingInventoryService.getStock(99L))
          .thenThrow(new EntityNotFoundException("Không tìm thấy tồn kho cho Topping ID: 99"));

      assertThatThrownBy(() -> mockMvc.perform(get(BASE + "/toppings/99")))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(EntityNotFoundException.class)
          .hasRootCauseMessage("Không tìm thấy tồn kho cho Topping ID: 99");
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1.5", "1e3" })
    @DisplayName("Trả về HTTP 400 Bad Request khi toppingId không phải số nguyên")
    void getToppingStock_InvalidId_ReturnsBadRequest(String id) throws Exception {
      mockMvc.perform(get(BASE + "/toppings/" + id))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }
  }

  // ==========================================
  // 2. POST /api/v1/inventory/toppings/import
  // ==========================================
  @Nested
  @DisplayName("POST /api/v1/inventory/toppings/import")
  class ImportToppingEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK và gọi addStock với lý do người dùng nhập")
    void importTopping_ReturnsOk_WithUserReason() throws Exception {
      postJson(BASE + "/toppings/import", "{\"toppingId\":1,\"amount\":10,\"reason\":\"Nhập hàng đợt 1\"}")
          .andExpect(status().isOk())
          .andExpect(content().string("Nhập kho Topping thành công! Số lượng nhập: 10"));

      verify(toppingInventoryService, times(1)).addStock(eq(1L), isNull(), eq(10), eq("Nhập hàng đợt 1"));
    }

    @Test
    @DisplayName("Không có reason thì dùng lý do mặc định IMPORT")
    void importTopping_UsesDefaultReason_WhenReasonMissing() throws Exception {
      postJson(BASE + "/toppings/import", "{\"toppingId\":1,\"amount\":10}")
          .andExpect(status().isOk());

      verify(toppingInventoryService).addStock(eq(1L), isNull(), eq(10), eq("IMPORT"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   ", "\\t" })
    @DisplayName("reason rỗng hoặc chỉ có khoảng trắng thì dùng lý do mặc định IMPORT")
    void importTopping_UsesDefaultReason_WhenReasonBlank(String reason) throws Exception {
      postJson(BASE + "/toppings/import", "{\"toppingId\":1,\"amount\":10,\"reason\":\"" + reason + "\"}")
          .andExpect(status().isOk());

      verify(toppingInventoryService).addStock(eq(1L), isNull(), eq(10), eq("IMPORT"));
    }

    @Test
    @DisplayName("reason = null thì dùng lý do mặc định IMPORT")
    void importTopping_UsesDefaultReason_WhenReasonNull() throws Exception {
      postJson(BASE + "/toppings/import", "{\"toppingId\":1,\"amount\":10,\"reason\":null}")
          .andExpect(status().isOk());

      verify(toppingInventoryService).addStock(eq(1L), isNull(), eq(10), eq("IMPORT"));
    }

    @Test
    @DisplayName("Biên: amount = 1 là hợp lệ")
    void importTopping_ReturnsOk_WhenAmountIsOne() throws Exception {
      postJson(BASE + "/toppings/import", "{\"toppingId\":1,\"amount\":1}")
          .andExpect(status().isOk())
          .andExpect(content().string("Nhập kho Topping thành công! Số lượng nhập: 1"));

      verify(toppingInventoryService).addStock(eq(1L), isNull(), eq(1), eq("IMPORT"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -5, Integer.MIN_VALUE })
    @DisplayName("Trả về HTTP 400 và message validate khi amount <= 0")
    void importTopping_AmountNotPositive_ReturnsBadRequest(int amount) throws Exception {
      postJson(BASE + "/toppings/import", "{\"toppingId\":1,\"amount\":" + amount + "}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu toppingId")
    void importTopping_MissingToppingId_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/toppings/import", "{\"amount\":5}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi body JSON sai cú pháp")
    void importTopping_MalformedJson_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/toppings/import", "{ day khong phai json")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi không có body")
    void importTopping_EmptyBody_ReturnsBadRequest() throws Exception {
      mockMvc.perform(post(BASE + "/toppings/import").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "null",
        "[]",
        "khong phai json",
        "{\"toppingId\":null,\"amount\":5}",
        "{\"toppingId\":\"abc\",\"amount\":5}",
        "{\"toppingId\":1,\"amount\":null}",
        "{\"toppingId\":1,\"amount\":\"abc\"}",
        "{\"toppingId\":1}"
    })
    @DisplayName("Nhiều kiểu body sai đều trả về HTTP 400 và không gọi service")
    void importTopping_VariousInvalidBodies_ReturnBadRequest(String json) throws Exception {
      postJson(BASE + "/toppings/import", json)
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 415 khi sai Content-Type")
    void importTopping_WrongContentType_ReturnsUnsupportedMediaType() throws Exception {
      mockMvc.perform(post(BASE + "/toppings/import")
          .contentType(MediaType.TEXT_PLAIN)
          .content("{\"toppingId\":1,\"amount\":10}"))
          .andExpect(status().isUnsupportedMediaType());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Topping không tồn tại: EntityNotFoundException bị ném ra ngoài (không có handler)")
    void importTopping_ToppingNotFound_ReturnsNotFound() throws Exception {
      doThrow(new EntityNotFoundException("Không tìm thấy topping với ID: 99"))
          .when(toppingInventoryService).addStock(eq(99L), isNull(), eq(5), eq("IMPORT"));

      assertThatThrownBy(() -> postJson(BASE + "/toppings/import", "{\"toppingId\":99,\"amount\":5}"))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(EntityNotFoundException.class)
          .hasRootCauseMessage("Không tìm thấy topping với ID: 99");
    }
  }

  // ==========================================
  // 3. POST /api/v1/inventory/toppings/deduct
  // ==========================================
  @Nested
  @DisplayName("POST /api/v1/inventory/toppings/deduct")
  class DeductToppingEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK và gọi deductStock")
    void deductTopping_ReturnsOk() throws Exception {
      postJson(BASE + "/toppings/deduct", "{\"toppingId\":1,\"amount\":3}")
          .andExpect(status().isOk())
          .andExpect(content().string("Trừ kho Topping thành công! Đã trừ: 3"));

      verify(toppingInventoryService, times(1)).deductStock(eq(1L), isNull(), eq(3));
    }

    @Test
    @DisplayName("Biên: amount = 1 là hợp lệ")
    void deductTopping_ReturnsOk_WhenAmountIsOne() throws Exception {
      postJson(BASE + "/toppings/deduct", "{\"toppingId\":1,\"amount\":1}")
          .andExpect(status().isOk());

      verify(toppingInventoryService).deductStock(eq(1L), isNull(), eq(1));
    }

    @Test
    @DisplayName("Không đủ hàng: IllegalArgumentException bị ném ra ngoài (không có handler)")
    void deductTopping_NotEnoughStock_ReturnsBadRequest() throws Exception {
      doThrow(new IllegalArgumentException("Topping không đủ số lượng tồn kho!"))
          .when(toppingInventoryService).deductStock(eq(1L), isNull(), eq(1000));

      assertThatThrownBy(() -> postJson(BASE + "/toppings/deduct", "{\"toppingId\":1,\"amount\":1000}"))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Topping không đủ số lượng tồn kho!");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -3, Integer.MIN_VALUE })
    @DisplayName("Trả về HTTP 400 và không gọi service khi amount <= 0")
    void deductTopping_AmountNotPositive_ReturnsBadRequest(int amount) throws Exception {
      postJson(BASE + "/toppings/deduct", "{\"toppingId\":1,\"amount\":" + amount + "}")
          .andExpect(status().isBadRequest());

      verify(toppingInventoryService, never()).deductStock(eq(1L), isNull(), eq(amount));
      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu toppingId")
    void deductTopping_MissingToppingId_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/toppings/deduct", "{\"amount\":4}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi body JSON sai cú pháp")
    void deductTopping_MalformedJson_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/toppings/deduct", "{ day khong phai json")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 415 khi sai Content-Type")
    void deductTopping_WrongContentType_ReturnsUnsupportedMediaType() throws Exception {
      mockMvc.perform(post(BASE + "/toppings/deduct")
          .contentType(MediaType.TEXT_PLAIN)
          .content("{\"toppingId\":1,\"amount\":3}"))
          .andExpect(status().isUnsupportedMediaType());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Topping không tồn tại: EntityNotFoundException bị ném ra ngoài (không có handler)")
    void deductTopping_ToppingNotFound_ReturnsNotFound() throws Exception {
      doThrow(new EntityNotFoundException("Không tìm thấy topping với ID: 99"))
          .when(toppingInventoryService).deductStock(eq(99L), isNull(), eq(3));

      assertThatThrownBy(() -> postJson(BASE + "/toppings/deduct", "{\"toppingId\":99,\"amount\":3}"))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(EntityNotFoundException.class)
          .hasRootCauseMessage("Không tìm thấy topping với ID: 99");
    }
  }

  // ==========================================
  // 4. GET /api/v1/inventory/toppings/check
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/inventory/toppings/check")
  class CheckToppingStockEndpointTests {

    @Test
    @DisplayName("Còn hàng: trả về HTTP 200 và true")
    void checkToppingStock_ReturnsTrue_WhenInStock() throws Exception {
      when(toppingInventoryService.checkStockAvailable(1L, 5)).thenReturn(true);

      mockMvc.perform(get(BASE + "/toppings/check").param("toppingId", "1").param("amount", "5"))
          .andExpect(status().isOk())
          .andExpect(content().string("true"));

      verify(toppingInventoryService, times(1)).checkStockAvailable(1L, 5);
    }

    @Test
    @DisplayName("Hết hàng: trả về HTTP 200 và false")
    void checkToppingStock_ReturnsFalse_WhenOutOfStock() throws Exception {
      when(toppingInventoryService.checkStockAvailable(1L, 5)).thenReturn(false);

      mockMvc.perform(get(BASE + "/toppings/check").param("toppingId", "1").param("amount", "5"))
          .andExpect(status().isOk())
          .andExpect(content().string("false"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "-5" })
    @DisplayName("Trả về HTTP 400 khi amount <= 0")
    void checkToppingStock_AmountNotPositive_ReturnsBadRequest(String amount) throws Exception {
      expectRejected(get(BASE + "/toppings/check").param("toppingId", "1").param("amount", amount));

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu amount")
    void checkToppingStock_MissingAmount_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/toppings/check").param("toppingId", "1"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu toppingId")
    void checkToppingStock_MissingToppingId_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/toppings/check").param("amount", "5"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu cả hai tham số")
    void checkToppingStock_MissingAllParams_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/toppings/check"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi toppingId không phải số")
    void checkToppingStock_InvalidToppingId_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/toppings/check").param("toppingId", "abc").param("amount", "5"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi amount không phải số")
    void checkToppingStock_InvalidAmount_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/toppings/check").param("toppingId", "1").param("amount", "abc"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingInventoryService);
    }
  }

  // ==========================================
  // 5. GET /api/v1/inventory/logs/toppings/{toppingId}
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/inventory/logs/toppings/{toppingId}")
  class GetToppingLogsEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng danh sách log")
    void getToppingLogs_ReturnsOk() throws Exception {
      InventoryLogResponse log = InventoryLogResponse.builder()
          .id(1L).toppingId(1L).toppingName("Trân châu")
          .quantityChange(10).changeType("IMPORT").build();
      when(inventoryLogService.getToppingLogs(1L)).thenReturn(List.of(log));

      mockMvc.perform(get(BASE + "/logs/toppings/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].toppingName").value("Trân châu"))
          .andExpect(jsonPath("$[0].quantityChange").value(10))
          .andExpect(jsonPath("$[0].changeType").value("IMPORT"));

      verify(inventoryLogService, times(1)).getToppingLogs(1L);
    }

    @Test
    @DisplayName("Nhiều log: giữ nguyên thứ tự service trả về")
    void getToppingLogs_KeepsOrder_ForMultipleLogs() throws Exception {
      InventoryLogResponse newer = InventoryLogResponse.builder()
          .id(2L).toppingId(1L).toppingName("Trân châu")
          .quantityChange(-3).changeType("ORDER_DEDUCT").build();
      InventoryLogResponse older = InventoryLogResponse.builder()
          .id(1L).toppingId(1L).toppingName("Trân châu")
          .quantityChange(10).changeType("IMPORT").build();
      when(inventoryLogService.getToppingLogs(1L)).thenReturn(List.of(newer, older));

      mockMvc.perform(get(BASE + "/logs/toppings/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(2))
          .andExpect(jsonPath("$[0].changeType").value("ORDER_DEDUCT"))
          .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    @DisplayName("Chưa có log: trả về HTTP 200 và mảng rỗng")
    void getToppingLogs_ReturnsEmptyArray_WhenNoLogs() throws Exception {
      when(inventoryLogService.getToppingLogs(1L)).thenReturn(Collections.emptyList());

      mockMvc.perform(get(BASE + "/logs/toppings/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi toppingId không phải số")
    void getToppingLogs_InvalidId_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/logs/toppings/abc"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(inventoryLogService);
    }
  }

  // ##########################################################
  //                         PRODUCT
  // ##########################################################

  // ==========================================
  // 6. GET /api/v1/inventory/products/{productId}
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/inventory/products/{productId}")
  class GetProductStockEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng thông tin tồn kho")
    void getProductStock_ReturnsOk() throws Exception {
      InventoryStockResponse response = InventoryStockResponse.builder()
          .itemId(2L).itemName("Cà phê sữa").stock(50).minThreshold(5).build();
      when(productInventoryService.getStock(2L)).thenReturn(response);

      mockMvc.perform(get(BASE + "/products/2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.itemId").value(2))
          .andExpect(jsonPath("$.itemName").value("Cà phê sữa"))
          .andExpect(jsonPath("$.stock").value(50))
          .andExpect(jsonPath("$.minThreshold").value(5));

      verify(productInventoryService, times(1)).getStock(2L);
    }

    @Test
    @DisplayName("Chưa có tồn kho: EntityNotFoundException bị ném ra ngoài (không có handler)")
    void getProductStock_NotFound_Returns404() throws Exception {
      when(productInventoryService.getStock(99L))
          .thenThrow(new EntityNotFoundException("Không tìm thấy tồn kho cho Product ID: 99"));

      assertThatThrownBy(() -> mockMvc.perform(get(BASE + "/products/99")))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(EntityNotFoundException.class)
          .hasRootCauseMessage("Không tìm thấy tồn kho cho Product ID: 99");
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1.5", "1e3" })
    @DisplayName("Trả về HTTP 400 Bad Request khi productId không phải số nguyên")
    void getProductStock_InvalidId_ReturnsBadRequest(String id) throws Exception {
      mockMvc.perform(get(BASE + "/products/" + id))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }
  }

  // ==========================================
  // 7. POST /api/v1/inventory/products/import
  // ==========================================
  @Nested
  @DisplayName("POST /api/v1/inventory/products/import")
  class ImportProductEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK và gọi addStock với lý do người dùng nhập")
    void importProduct_ReturnsOk_WithUserReason() throws Exception {
      postJson(BASE + "/products/import", "{\"productId\":2,\"amount\":20,\"reason\":\"Nhập hàng\"}")
          .andExpect(status().isOk())
          .andExpect(content().string("Nhập kho Product thành công! Số lượng nhập: 20"));

      verify(productInventoryService, times(1)).addStock(eq(2L), isNull(), eq(20), eq("Nhập hàng"));
    }

    @Test
    @DisplayName("Không có reason thì dùng lý do mặc định IMPORT")
    void importProduct_UsesDefaultReason_WhenReasonMissing() throws Exception {
      postJson(BASE + "/products/import", "{\"productId\":2,\"amount\":20}")
          .andExpect(status().isOk());

      verify(productInventoryService).addStock(eq(2L), isNull(), eq(20), eq("IMPORT"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   " })
    @DisplayName("reason rỗng hoặc chỉ có khoảng trắng thì dùng lý do mặc định IMPORT")
    void importProduct_UsesDefaultReason_WhenReasonBlank(String reason) throws Exception {
      postJson(BASE + "/products/import", "{\"productId\":2,\"amount\":20,\"reason\":\"" + reason + "\"}")
          .andExpect(status().isOk());

      verify(productInventoryService).addStock(eq(2L), isNull(), eq(20), eq("IMPORT"));
    }

    @Test
    @DisplayName("Biên: amount = 1 là hợp lệ")
    void importProduct_ReturnsOk_WhenAmountIsOne() throws Exception {
      postJson(BASE + "/products/import", "{\"productId\":2,\"amount\":1}")
          .andExpect(status().isOk())
          .andExpect(content().string("Nhập kho Product thành công! Số lượng nhập: 1"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -5, Integer.MIN_VALUE })
    @DisplayName("Trả về HTTP 400 và message validate khi amount <= 0")
    void importProduct_AmountNotPositive_ReturnsBadRequest(int amount) throws Exception {
      postJson(BASE + "/products/import", "{\"productId\":2,\"amount\":" + amount + "}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu productId")
    void importProduct_MissingProductId_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/products/import", "{\"amount\":5}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi body JSON sai cú pháp")
    void importProduct_MalformedJson_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/products/import", "{ day khong phai json")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi không có body")
    void importProduct_EmptyBody_ReturnsBadRequest() throws Exception {
      mockMvc.perform(post(BASE + "/products/import").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "null",
        "[]",
        "khong phai json",
        "{\"productId\":null,\"amount\":5}",
        "{\"productId\":\"abc\",\"amount\":5}",
        "{\"productId\":2,\"amount\":null}",
        "{\"productId\":2,\"amount\":\"abc\"}",
        "{\"productId\":2}"
    })
    @DisplayName("Nhiều kiểu body sai đều trả về HTTP 400 và không gọi service")
    void importProduct_VariousInvalidBodies_ReturnBadRequest(String json) throws Exception {
      postJson(BASE + "/products/import", json)
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 415 khi sai Content-Type")
    void importProduct_WrongContentType_ReturnsUnsupportedMediaType() throws Exception {
      mockMvc.perform(post(BASE + "/products/import")
          .contentType(MediaType.TEXT_PLAIN)
          .content("{\"productId\":2,\"amount\":10}"))
          .andExpect(status().isUnsupportedMediaType());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Sản phẩm không tồn tại: EntityNotFoundException bị ném ra ngoài (không có handler)")
    void importProduct_ProductNotFound_ReturnsNotFound() throws Exception {
      doThrow(new EntityNotFoundException("Không tìm thấy sản phẩm với ID: 99"))
          .when(productInventoryService).addStock(eq(99L), isNull(), eq(5), eq("IMPORT"));

      assertThatThrownBy(() -> postJson(BASE + "/products/import", "{\"productId\":99,\"amount\":5}"))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(EntityNotFoundException.class)
          .hasRootCauseMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 8. POST /api/v1/inventory/products/deduct
  // ==========================================
  @Nested
  @DisplayName("POST /api/v1/inventory/products/deduct")
  class DeductProductEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK và gọi deductStock")
    void deductProduct_ReturnsOk() throws Exception {
      postJson(BASE + "/products/deduct", "{\"productId\":2,\"amount\":4}")
          .andExpect(status().isOk())
          .andExpect(content().string("Trừ kho Product thành công! Đã trừ: 4"));

      verify(productInventoryService, times(1)).deductStock(eq(2L), isNull(), eq(4));
    }

    @Test
    @DisplayName("Biên: amount = 1 là hợp lệ")
    void deductProduct_ReturnsOk_WhenAmountIsOne() throws Exception {
      postJson(BASE + "/products/deduct", "{\"productId\":2,\"amount\":1}")
          .andExpect(status().isOk());

      verify(productInventoryService).deductStock(eq(2L), isNull(), eq(1));
    }

    @Test
    @DisplayName("Không đủ hàng: IllegalArgumentException bị ném ra ngoài (không có handler)")
    void deductProduct_NotEnoughStock_ReturnsBadRequest() throws Exception {
      doThrow(new IllegalArgumentException("Sản phẩm không đủ số lượng tồn kho!"))
          .when(productInventoryService).deductStock(eq(2L), isNull(), eq(999));

      assertThatThrownBy(() -> postJson(BASE + "/products/deduct", "{\"productId\":2,\"amount\":999}"))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Sản phẩm không đủ số lượng tồn kho!");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -3, Integer.MIN_VALUE })
    @DisplayName("Trả về HTTP 400 và không gọi service khi amount <= 0")
    void deductProduct_AmountNotPositive_ReturnsBadRequest(int amount) throws Exception {
      postJson(BASE + "/products/deduct", "{\"productId\":2,\"amount\":" + amount + "}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu productId")
    void deductProduct_MissingProductId_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/products/deduct", "{\"amount\":4}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi body JSON sai cú pháp")
    void deductProduct_MalformedJson_ReturnsBadRequest() throws Exception {
      postJson(BASE + "/products/deduct", "{ day khong phai json")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 415 khi sai Content-Type")
    void deductProduct_WrongContentType_ReturnsUnsupportedMediaType() throws Exception {
      mockMvc.perform(post(BASE + "/products/deduct")
          .contentType(MediaType.TEXT_PLAIN)
          .content("{\"productId\":2,\"amount\":4}"))
          .andExpect(status().isUnsupportedMediaType());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Sản phẩm không tồn tại: EntityNotFoundException bị ném ra ngoài (không có handler)")
    void deductProduct_ProductNotFound_ReturnsNotFound() throws Exception {
      doThrow(new EntityNotFoundException("Không tìm thấy sản phẩm với ID: 99"))
          .when(productInventoryService).deductStock(eq(99L), isNull(), eq(3));

      assertThatThrownBy(() -> postJson(BASE + "/products/deduct", "{\"productId\":99,\"amount\":3}"))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(EntityNotFoundException.class)
          .hasRootCauseMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 9. GET /api/v1/inventory/products/check
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/inventory/products/check")
  class CheckProductStockEndpointTests {

    @Test
    @DisplayName("Còn hàng: trả về HTTP 200 và true")
    void checkProductStock_ReturnsTrue_WhenInStock() throws Exception {
      when(productInventoryService.checkStockAvailable(2L, 3)).thenReturn(true);

      mockMvc.perform(get(BASE + "/products/check").param("productId", "2").param("amount", "3"))
          .andExpect(status().isOk())
          .andExpect(content().string("true"));

      verify(productInventoryService, times(1)).checkStockAvailable(2L, 3);
    }

    @Test
    @DisplayName("Hết hàng: trả về HTTP 200 và false")
    void checkProductStock_ReturnsFalse_WhenOutOfStock() throws Exception {
      when(productInventoryService.checkStockAvailable(2L, 3)).thenReturn(false);

      mockMvc.perform(get(BASE + "/products/check").param("productId", "2").param("amount", "3"))
          .andExpect(status().isOk())
          .andExpect(content().string("false"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "-5" })
    @DisplayName("Trả về HTTP 400 khi amount <= 0")
    void checkProductStock_AmountNotPositive_ReturnsBadRequest(String amount) throws Exception {
      expectRejected(get(BASE + "/products/check").param("productId", "2").param("amount", amount));

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi amount không phải số")
    void checkProductStock_InvalidAmount_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/products/check").param("productId", "2").param("amount", "abc"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi productId không phải số")
    void checkProductStock_InvalidProductId_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/products/check").param("productId", "abc").param("amount", "3"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu productId")
    void checkProductStock_MissingProductId_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/products/check").param("amount", "3"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu amount")
    void checkProductStock_MissingAmount_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/products/check").param("productId", "2"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(productInventoryService);
    }
  }

  // ==========================================
  // 10. GET /api/v1/inventory/logs/products/{productId}
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/inventory/logs/products/{productId}")
  class GetProductLogsEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng danh sách log")
    void getProductLogs_ReturnsOk() throws Exception {
      InventoryLogResponse log = InventoryLogResponse.builder()
          .id(1L).productId(2L).productName("Cà phê sữa")
          .quantityChange(-3).changeType("ORDER_DEDUCT").build();
      when(inventoryLogService.getProductLogs(2L)).thenReturn(List.of(log));

      mockMvc.perform(get(BASE + "/logs/products/2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].productName").value("Cà phê sữa"))
          .andExpect(jsonPath("$[0].quantityChange").value(-3))
          .andExpect(jsonPath("$[0].changeType").value("ORDER_DEDUCT"));

      verify(inventoryLogService, times(1)).getProductLogs(2L);
    }

    @Test
    @DisplayName("Chưa có log: trả về HTTP 200 và mảng rỗng")
    void getProductLogs_ReturnsEmptyArray_WhenNoLogs() throws Exception {
      when(inventoryLogService.getProductLogs(2L)).thenReturn(Collections.emptyList());

      mockMvc.perform(get(BASE + "/logs/products/2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi productId không phải số")
    void getProductLogs_InvalidId_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get(BASE + "/logs/products/abc"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(inventoryLogService);
    }
  }
}