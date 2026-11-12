package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import vn.doancnpm.controller.ToppingController;
import vn.doancnpm.dto.Request.ToppingRequest;
import vn.doancnpm.dto.Response.ToppingResponse;
import vn.doancnpm.service.ToppingService;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToppingControllerTest {

  private static final String BASE = "/api/v1/toppings";
  private static final String TOPPING_JSON = "{\"name\":\"Trân châu đen\",\"unitPrice\":5000}";


  private MockMvc mockMvc;

  // findAndRegisterModules: để serialize được LocalDateTime (timestamp) trong body lỗi
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private ToppingService toppingService;

  @InjectMocks
  private ToppingController toppingController;

  @BeforeEach
  @SuppressWarnings({ "removal", "deprecation" })
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(toppingController)
        .setValidator(validator)
        .setMessageConverters(
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
        .build();
  }

  // ---------- Helpers ----------

  private ToppingResponse sampleResponse() {
    return ToppingResponse.builder()
        .id(1L).name("Trân châu đen").unitPrice(new BigDecimal("5000"))
        .stock(20).status(true).build();
  }

  /** Tạo body JSON {name, unitPrice}. Tự escape dấu " và \ trong name. */
  private String body(String name, String price) {
    String escaped = name.replace("\\", "\\\\").replace("\"", "\\\"");
    return "{\"name\":\"" + escaped + "\",\"unitPrice\":" + price + "}";
  }

  private ResultActions postJson(String url, String body) throws Exception {
    return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private ResultActions putJson(String url, String body) throws Exception {
    return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  // ==========================================
  // 1. GET /api/v1/toppings
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/toppings")
  class GetAllToppingsEndpointTests {

    @Test
    @DisplayName("Không có status: truyền null cho service và trả về HTTP 200")
    void getAllToppings_NoStatus_PassesNullToService() throws Exception {
      when(toppingService.getAllToppings(null)).thenReturn(List.of(sampleResponse()));

      mockMvc.perform(get(BASE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].name").value("Trân châu đen"))
          .andExpect(jsonPath("$[0].stock").value(20));

      verify(toppingService, times(1)).getAllToppings(null);
    }

    @Test
    @DisplayName("status = true: truyền true cho service")
    void getAllToppings_StatusTrue_PassesTrueToService() throws Exception {
      when(toppingService.getAllToppings(true)).thenReturn(Collections.emptyList());

      mockMvc.perform(get(BASE).param("status", "true"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));

      verify(toppingService).getAllToppings(true);
    }

    @Test
    @DisplayName("status = false: truyền false cho service")
    void getAllToppings_StatusFalse_PassesFalseToService() throws Exception {
      when(toppingService.getAllToppings(false)).thenReturn(Collections.emptyList());

      mockMvc.perform(get(BASE).param("status", "false"))
          .andExpect(status().isOk());

      verify(toppingService).getAllToppings(false);
    }

    @Test
    @DisplayName("Nhiều topping: trả về đủ và đúng thứ tự")
    void getAllToppings_ReturnsAllInOrder() throws Exception {
      ToppingResponse second = ToppingResponse.builder()
          .id(2L).name("Thạch dừa").unitPrice(new BigDecimal("4000")).stock(0).status(false).build();
      when(toppingService.getAllToppings(null)).thenReturn(List.of(sampleResponse(), second));

      mockMvc.perform(get(BASE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[1].id").value(2))
          .andExpect(jsonPath("$[1].status").value(false));
    }

    @Test
    @DisplayName("Chưa có topping nào: trả về HTTP 200 và mảng rỗng")
    void getAllToppings_ReturnsEmptyArray_WhenNoData() throws Exception {
      when(toppingService.getAllToppings(null)).thenReturn(Collections.emptyList());

      mockMvc.perform(get(BASE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "2", "yesno" })
    @DisplayName("Trả về HTTP 400 khi status không phải boolean")
    void getAllToppings_InvalidStatus_ReturnsBadRequest(String status) throws Exception {
      mockMvc.perform(get(BASE).param("status", status))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }
  }

  // ==========================================
  // 2. GET /api/v1/toppings/{id}
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/toppings/{id}")
  class GetToppingByIdEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng thông tin topping")
    void getToppingById_ReturnsOk() throws Exception {
      when(toppingService.getToppingById(1L)).thenReturn(sampleResponse());

      mockMvc.perform(get(BASE + "/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Trân châu đen"))
          .andExpect(jsonPath("$.unitPrice").value(5000))
          .andExpect(jsonPath("$.stock").value(20))
          .andExpect(jsonPath("$.status").value(true));

      verify(toppingService, times(1)).getToppingById(1L);
    }

    @Test
    @DisplayName("Service ném RuntimeException (không có handler): exception bị ném ra ngoài")
    void getToppingById_ServiceThrowsRuntimeException_IsPropagated() {
      // Handler riêng của test không xử lý RuntimeException thường nên nó bị ném ra ngoài.
      // Nếu service đổi sang EntityNotFoundException thì sửa test này thành 404.
      when(toppingService.getToppingById(99L))
          .thenThrow(new RuntimeException("Không tìm thấy Topping với ID: 99"));

      assertThatThrownBy(() -> mockMvc.perform(get(BASE + "/99")))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("Không tìm thấy Topping với ID: 99");
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1.5", "1e3" })
    @DisplayName("Trả về HTTP 400 khi id không phải số nguyên")
    void getToppingById_InvalidId_ReturnsBadRequest(String id) throws Exception {
      mockMvc.perform(get(BASE + "/" + id))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }
  }

  // ==========================================
  // 3. POST /api/v1/toppings
  // ==========================================
  @Nested
  @DisplayName("POST /api/v1/toppings")
  class CreateToppingEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 201 Created và truyền đúng dữ liệu cho service")
    void createTopping_ReturnsCreated_AndPassesCorrectData() throws Exception {
      when(toppingService.createTopping(any(ToppingRequest.class))).thenReturn(sampleResponse());

      postJson(BASE, TOPPING_JSON)
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Trân châu đen"));

      ArgumentCaptor<ToppingRequest> captor = ArgumentCaptor.forClass(ToppingRequest.class);
      verify(toppingService, times(1)).createTopping(captor.capture());
      assertThat(captor.getValue().getName()).isEqualTo("Trân châu đen");
      assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Service ném IllegalArgumentException (không có handler): exception bị ném ra ngoài")
    void createTopping_ServiceThrowsIllegalArgument_IsPropagated() throws Exception {
      when(toppingService.createTopping(any(ToppingRequest.class)))
          .thenThrow(new IllegalArgumentException("Tên topping đã tồn tại"));

      assertThatThrownBy(() -> postJson(BASE, TOPPING_JSON))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Tên topping đã tồn tại");
    }

    // ---------- Tên hợp lệ ----------

    @ParameterizedTest
    @ValueSource(strings = { "Trân châu", "Trà sữa 2", "Matcha Latte", "Thạch dừa", "ÉCLAIR", "A" })
    @DisplayName("Tên hợp lệ (chữ, số, khoảng trắng, dấu tiếng Việt) trả về HTTP 201")
    void createTopping_ValidName_ReturnsCreated(String name) throws Exception {
      when(toppingService.createTopping(any(ToppingRequest.class))).thenReturn(sampleResponse());

      postJson(BASE, body(name, "5000"))
          .andExpect(status().isCreated());

      ArgumentCaptor<ToppingRequest> captor = ArgumentCaptor.forClass(ToppingRequest.class);
      verify(toppingService).createTopping(captor.capture());
      assertThat(captor.getValue().getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("Biên: tên dài đúng 100 ký tự trả về HTTP 201")
    void createTopping_NameIsMaxLength_ReturnsCreated() throws Exception {
      when(toppingService.createTopping(any(ToppingRequest.class))).thenReturn(sampleResponse());

      postJson(BASE, body("a".repeat(100), "5000"))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Biên: giá nhỏ nhất > 0 trả về HTTP 201")
    void createTopping_SmallestPositivePrice_ReturnsCreated() throws Exception {
      when(toppingService.createTopping(any(ToppingRequest.class))).thenReturn(sampleResponse());

      postJson(BASE, body("Pudding", "1"))
          .andExpect(status().isCreated());
    }

    // ---------- Tên sai (CẦN @Pattern / @Size trong ToppingRequest) ----------

    @ParameterizedTest
    @ValueSource(strings = {
        "Pudding!", "Trân@châu", "Trà#sữa", "Tea$", "100%", "A^B", "A&B", "A*B",
        "Trà (đào)", "Trà-sữa", "Trà_sữa", "Trà+sữa", "Trà=sữa", "Trà/sữa", "Trà\\sữa",
        "Trà.sữa", "Trà,sữa", "Trà;sữa", "Trà:sữa", "Trà'sữa", "Trà\"sữa", "Trà?sữa",
        "<script>alert(1)</script>", "Robert'); DROP TABLE topping;--", "Trà~sữa",
        "Trà|sữa", "Trà{sữa}", "Trà[sữa]", "😀 Trà", "Trà 🧋", "Trà ★"
    })
    @DisplayName("Trả về HTTP 400 khi tên chứa ký tự đặc biệt")
    void createTopping_NameHasSpecialCharacters_ReturnsBadRequest(String name) throws Exception {
      postJson(BASE, body(name, "5000"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Biên: tên dài 101 ký tự trả về HTTP 400")
    void createTopping_NameTooLong_ReturnsBadRequest() throws Exception {
      postJson(BASE, body("a".repeat(101), "5000"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    // ---------- Tên rỗng / thiếu ----------

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu name")
    void createTopping_MissingName_ReturnsBadRequest() throws Exception {
      postJson(BASE, "{\"unitPrice\":5000}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi name = null")
    void createTopping_NameNull_ReturnsBadRequest() throws Exception {
      postJson(BASE, "{\"name\":null,\"unitPrice\":5000}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   " })
    @DisplayName("Trả về HTTP 400 khi name rỗng hoặc chỉ có khoảng trắng")
    void createTopping_NameBlank_ReturnsBadRequest(String name) throws Exception {
      // Chuỗi rỗng có thể vi phạm đồng thời @NotBlank và @Pattern; handler chỉ lấy lỗi đầu tiên
      // nên chấp nhận 1 trong 2 message.
      postJson(BASE, "{\"name\":\"" + name + "\",\"unitPrice\":5000}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    // ---------- Giá sai ----------

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu giá")
    void createTopping_MissingPrice_ReturnsBadRequest() throws Exception {
      postJson(BASE, "{\"name\":\"Pudding\"}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi giá = null")
    void createTopping_PriceNull_ReturnsBadRequest() throws Exception {
      postJson(BASE, "{\"name\":\"Pudding\",\"unitPrice\":null}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "0.0", "-1", "-1000", "-0.01" })
    @DisplayName("Trả về HTTP 400 khi giá <= 0")
    void createTopping_PriceNotPositive_ReturnsBadRequest(String price) throws Exception {
      postJson(BASE, body("Pudding", price))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi giá sai kiểu dữ liệu")
    void createTopping_PriceWrongType_ReturnsBadRequest() throws Exception {
      postJson(BASE, "{\"name\":\"Pudding\",\"unitPrice\":\"abc\"}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    // ---------- Body / header sai ----------

    @Test
    @DisplayName("Trả về HTTP 400 khi JSON sai cú pháp")
    void createTopping_MalformedJson_ReturnsBadRequest() throws Exception {
      postJson(BASE, "{\"name\": \"Trân châu\", ")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi không có body")
    void createTopping_EmptyBody_ReturnsBadRequest() throws Exception {
      mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 415 khi sai Content-Type")
    void createTopping_WrongContentType_ReturnsUnsupportedMediaType() throws Exception {
      mockMvc.perform(post(BASE).contentType(MediaType.TEXT_PLAIN).content(TOPPING_JSON))
          .andExpect(status().isUnsupportedMediaType());

      verifyNoInteractions(toppingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "null",
        "[]",
        "[1,2,3]",
        "\"chuoi\"",
        "khong phai json",
        "{\"name\":\"\",\"unitPrice\":5000}",
        "{\"name\":null,\"unitPrice\":5000}",
        "{\"name\":\"A\",\"unitPrice\":null}",
        "{\"name\":\"A\",\"unitPrice\":0}",
        "{\"name\":\"A\",\"unitPrice\":-5}",
        "{\"name\":\"A\",\"unitPrice\":\"abc\"}",
        "{\"name\":\"A\",\"unitPrice\":true}"
    })
    @DisplayName("Nhiều kiểu body sai đều trả về HTTP 400 và không gọi service")
    void createTopping_VariousInvalidBodies_ReturnBadRequest(String json) throws Exception {
      postJson(BASE, json)
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }
  }

  // ==========================================
  // 4. PUT /api/v1/toppings/{id}
  // ==========================================
  @Nested
  @DisplayName("PUT /api/v1/toppings/{id}")
  class UpdateToppingEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK và truyền đúng id + dữ liệu cho service")
    void updateTopping_ReturnsOk_AndPassesCorrectData() throws Exception {
      when(toppingService.updateTopping(eq(1L), any(ToppingRequest.class))).thenReturn(sampleResponse());

      putJson(BASE + "/1", TOPPING_JSON)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("Trân châu đen"));

      ArgumentCaptor<ToppingRequest> captor = ArgumentCaptor.forClass(ToppingRequest.class);
      verify(toppingService, times(1)).updateTopping(eq(1L), captor.capture());
      assertThat(captor.getValue().getName()).isEqualTo("Trân châu đen");
      assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Service ném RuntimeException (không có handler): exception bị ném ra ngoài")
    void updateTopping_ServiceThrowsRuntimeException_IsPropagated() {
      when(toppingService.updateTopping(eq(99L), any(ToppingRequest.class)))
          .thenThrow(new RuntimeException("Không tìm thấy Topping với ID: 99"));

      assertThatThrownBy(() -> putJson(BASE + "/99", TOPPING_JSON))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("Không tìm thấy Topping với ID: 99");
    }

    @Test
    @DisplayName("Service ném IllegalArgumentException (không có handler): exception bị ném ra ngoài")
    void updateTopping_ServiceThrowsIllegalArgument_IsPropagated() throws Exception {
      when(toppingService.updateTopping(eq(1L), any(ToppingRequest.class)))
          .thenThrow(new IllegalArgumentException("Tên topping đã tồn tại"));

      assertThatThrownBy(() -> putJson(BASE + "/1", TOPPING_JSON))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasRootCauseMessage("Tên topping đã tồn tại");
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1.5", "1e3" })
    @DisplayName("Trả về HTTP 400 khi id không phải số nguyên")
    void updateTopping_InvalidId_ReturnsBadRequest(String id) throws Exception {
      putJson(BASE + "/" + id, TOPPING_JSON)
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    // ---------- Tên ----------

    @ParameterizedTest
    @ValueSource(strings = { "Trân châu trắng", "Trà sữa 2", "Matcha Latte" })
    @DisplayName("Tên hợp lệ trả về HTTP 200")
    void updateTopping_ValidName_ReturnsOk(String name) throws Exception {
      when(toppingService.updateTopping(eq(1L), any(ToppingRequest.class))).thenReturn(sampleResponse());

      putJson(BASE + "/1", body(name, "7000"))
          .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Pudding!", "Trân@châu", "Trà#sữa", "Tea$", "<b>Trà</b>", "Trà-sữa", "Trà_sữa",
        "Robert'); DROP TABLE topping;--", "😀 Trà"
    })
    @DisplayName("Trả về HTTP 400 khi tên chứa ký tự đặc biệt")
    void updateTopping_NameHasSpecialCharacters_ReturnsBadRequest(String name) throws Exception {
      putJson(BASE + "/1", body(name, "7000"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi tên dài hơn 100 ký tự")
    void updateTopping_NameTooLong_ReturnsBadRequest() throws Exception {
      putJson(BASE + "/1", body("a".repeat(101), "7000"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu name")
    void updateTopping_MissingName_ReturnsBadRequest() throws Exception {
      putJson(BASE + "/1", "{\"unitPrice\":5000}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   " })
    @DisplayName("Trả về HTTP 400 khi name rỗng hoặc chỉ có khoảng trắng")
    void updateTopping_NameBlank_ReturnsBadRequest(String name) throws Exception {
      putJson(BASE + "/1", "{\"name\":\"" + name + "\",\"unitPrice\":5000}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    // ---------- Giá ----------

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu giá")
    void updateTopping_MissingPrice_ReturnsBadRequest() throws Exception {
      putJson(BASE + "/1", "{\"name\":\"Pudding\"}")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "-7000" })
    @DisplayName("Trả về HTTP 400 khi giá <= 0")
    void updateTopping_PriceNotPositive_ReturnsBadRequest(String price) throws Exception {
      putJson(BASE + "/1", body("Pudding", price))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    // ---------- Body / header sai ----------

    @Test
    @DisplayName("Trả về HTTP 400 khi JSON sai cú pháp")
    void updateTopping_MalformedJson_ReturnsBadRequest() throws Exception {
      putJson(BASE + "/1", "khong phai json")
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi không có body")
    void updateTopping_EmptyBody_ReturnsBadRequest() throws Exception {
      mockMvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @Test
    @DisplayName("Trả về HTTP 415 khi sai Content-Type")
    void updateTopping_WrongContentType_ReturnsUnsupportedMediaType() throws Exception {
      mockMvc.perform(put(BASE + "/1").contentType(MediaType.TEXT_PLAIN).content(TOPPING_JSON))
          .andExpect(status().isUnsupportedMediaType());

      verifyNoInteractions(toppingService);
    }
  }

  // ==========================================
  // 5. PATCH /api/v1/toppings/{id}/status
  // ==========================================
  @Nested
  @DisplayName("PATCH /api/v1/toppings/{id}/status")
  class UpdateToppingStatusEndpointTests {

    @Test
    @DisplayName("Tắt topping: trả về HTTP 200 và status = false")
    void updateToppingStatus_TurnOff_ReturnsOk() throws Exception {
      ToppingResponse inactive = ToppingResponse.builder()
          .id(1L).name("Trân châu đen").status(false).build();
      when(toppingService.updateStatus(1L, false)).thenReturn(inactive);

      mockMvc.perform(patch(BASE + "/1/status").param("status", "false"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(false));

      verify(toppingService, times(1)).updateStatus(1L, false);
    }

    @Test
    @DisplayName("Bật topping: trả về HTTP 200 và status = true")
    void updateToppingStatus_TurnOn_ReturnsOk() throws Exception {
      when(toppingService.updateStatus(2L, true)).thenReturn(sampleResponse());

      mockMvc.perform(patch(BASE + "/2/status").param("status", "true"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(true));

      verify(toppingService, times(1)).updateStatus(2L, true);
    }

    @Test
    @DisplayName("Service ném RuntimeException (không có handler): exception bị ném ra ngoài")
    void updateToppingStatus_ServiceThrowsRuntimeException_IsPropagated() {
      when(toppingService.updateStatus(99L, true))
          .thenThrow(new RuntimeException("Không tìm thấy Topping với ID: 99"));

      assertThatThrownBy(() -> mockMvc.perform(patch(BASE + "/99/status").param("status", "true")))
          .isInstanceOf(ServletException.class)
          .hasRootCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("Không tìm thấy Topping với ID: 99");
    }

    @Test
    @DisplayName("Trả về HTTP 400 khi thiếu param status")
    void updateToppingStatus_MissingParam_ReturnsBadRequest() throws Exception {
      mockMvc.perform(patch(BASE + "/1/status"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "2", "" })
    @DisplayName("Trả về HTTP 400 khi status không phải boolean")
    void updateToppingStatus_InvalidStatus_ReturnsBadRequest(String status) throws Exception {
      mockMvc.perform(patch(BASE + "/1/status").param("status", status))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "1.5", "1e3" })
    @DisplayName("Trả về HTTP 400 khi id không phải số nguyên")
    void updateToppingStatus_InvalidId_ReturnsBadRequest(String id) throws Exception {
      mockMvc.perform(patch(BASE + "/" + id + "/status").param("status", "true"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(toppingService);
    }
  }
}