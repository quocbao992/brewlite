package vn.doancnpm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.controller.CategoryController;
import vn.doancnpm.dto.Request.CategoryRequest;
import vn.doancnpm.dto.Response.CategoryResponse;
import vn.doancnpm.service.CategoryService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

  private MockMvc mockMvc;

  // Body lỗi của controller chứa trường "timestamp" kiểu LocalDateTime.
  // ObjectMapper trơn không serialize được kiểu này, nên đăng ký serializer
  // (ghi ra chuỗi ISO) ngay trong test, không phụ thuộc module jsr310.
  private ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new SimpleModule().addSerializer(LocalDateTime.class, ToStringSerializer.instance));

  @Mock
  private CategoryService categoryService;

  @InjectMocks
  private CategoryController categoryController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
        .setMessageConverters(
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new MappingJackson2HttpMessageConverter(objectMapper))
        .build();
  }

  private CategoryResponse response(Long id, String name, Boolean status, Long displayOrder) {
    return new CategoryResponse(id, name, status, displayOrder);
  }

  // ==========================================
  // 1. POST /api/v1/categories
  // ==========================================
  @Nested
  @DisplayName("POST /api/v1/categories")
  class CreateEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 201 Created cùng dữ liệu danh mục vừa tạo")
    void create_ReturnsCreated() throws Exception {
      CategoryRequest request = new CategoryRequest("Trà sữa", true, 1L);

      when(categoryService.create(any(CategoryRequest.class)))
          .thenReturn(response(1L, "Trà sữa", true, 1L));

      mockMvc
          .perform(
              post("/api/v1/categories")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Trà sữa"))
          .andExpect(jsonPath("$.status").value(true))
          .andExpect(jsonPath("$.displayOrder").value(1));

      // Kiểm tra request JSON được bind đúng vào CategoryRequest
      ArgumentCaptor<CategoryRequest> captor = ArgumentCaptor.forClass(CategoryRequest.class);
      verify(categoryService, times(1)).create(captor.capture());
      assertThat(captor.getValue().getName()).isEqualTo("Trà sữa");
      assertThat(captor.getValue().getStatus()).isTrue();
      assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi service ném IllegalArgumentException")
    void create_IllegalArgument_ReturnsBadRequest() throws Exception {
      CategoryRequest request = new CategoryRequest("Trà sữa", true, 1L);

      when(categoryService.create(any(CategoryRequest.class)))
          .thenThrow(new IllegalArgumentException("Tên danh mục đã tồn tại"));

      mockMvc
          .perform(
              post("/api/v1/categories")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.message").value("Tên danh mục đã tồn tại"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi thiếu request body")
    void create_MissingBody_ReturnsBadRequest() throws Exception {
      mockMvc
          .perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verify(categoryService, never()).create(any());
    }
  }

  // ==========================================
  // 2. PUT /api/v1/categories/{id}
  // ==========================================
  @Nested
  @DisplayName("PUT /api/v1/categories/{id}")
  class UpdateEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi cập nhật thành công")
    void update_ReturnsOk() throws Exception {
      CategoryRequest request = new CategoryRequest("Cà phê", true, 2L);

      when(categoryService.update(eq(1L), any(CategoryRequest.class)))
          .thenReturn(response(1L, "Cà phê", true, 2L));

      mockMvc
          .perform(
              put("/api/v1/categories/{id}", 1L)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Cà phê"))
          .andExpect(jsonPath("$.displayOrder").value(2));

      verify(categoryService, times(1)).update(eq(1L), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("Trả về HTTP 404 Not Found khi danh mục không tồn tại")
    void update_NotFound_ReturnsNotFound() throws Exception {
      CategoryRequest request = new CategoryRequest("Cà phê", true, 2L);

      when(categoryService.update(eq(99L), any(CategoryRequest.class)))
          .thenThrow(new EntityNotFoundException("Không tìm thấy danh mục"));

      mockMvc
          .perform(
              put("/api/v1/categories/{id}", 99L)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.error").value("Not Found"))
          .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục"));
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi dữ liệu không hợp lệ")
    void update_IllegalArgument_ReturnsBadRequest() throws Exception {
      CategoryRequest request = new CategoryRequest("Cà phê", true, 2L);

      when(categoryService.update(eq(1L), any(CategoryRequest.class)))
          .thenThrow(new IllegalArgumentException("Tên danh mục đã tồn tại"));

      mockMvc
          .perform(
              put("/api/v1/categories/{id}", 1L)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Tên danh mục đã tồn tại"));
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi id không phải số")
    void update_InvalidIdType_ReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              put("/api/v1/categories/{id}", "abc")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new CategoryRequest("A", true, 1L))))
          .andExpect(status().isBadRequest());

      verify(categoryService, never()).update(any(), any());
    }
  }

  // ==========================================
  // 3. PATCH /api/v1/categories/{id}/hide
  // ==========================================
  @Nested
  @DisplayName("PATCH /api/v1/categories/{id}/hide")
  class HideEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi ẩn danh mục thành công")
    void hide_ReturnsOk() throws Exception {
      when(categoryService.hide(1L)).thenReturn(response(1L, "Trà sữa", false, 1L));

      mockMvc
          .perform(patch("/api/v1/categories/{id}/hide", 1L))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.status").value(false));

      verify(categoryService, times(1)).hide(1L);
    }

    @Test
    @DisplayName("Trả về HTTP 404 Not Found khi ẩn danh mục không tồn tại")
    void hide_NotFound_ReturnsNotFound() throws Exception {
      when(categoryService.hide(99L)).thenThrow(new EntityNotFoundException("Không tìm thấy danh mục"));

      mockMvc
          .perform(patch("/api/v1/categories/{id}/hide", 99L))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục"));
    }
  }

  // ==========================================
  // 4. PATCH /api/v1/categories/{id}/show
  // ==========================================
  @Nested
  @DisplayName("PATCH /api/v1/categories/{id}/show")
  class ShowEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi hiện danh mục thành công")
    void show_ReturnsOk() throws Exception {
      when(categoryService.show(1L)).thenReturn(response(1L, "Trà sữa", true, 1L));

      mockMvc
          .perform(patch("/api/v1/categories/{id}/show", 1L))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.status").value(true));

      verify(categoryService, times(1)).show(1L);
    }

    @Test
    @DisplayName("Trả về HTTP 404 Not Found khi hiện danh mục không tồn tại")
    void show_NotFound_ReturnsNotFound() throws Exception {
      when(categoryService.show(99L)).thenThrow(new EntityNotFoundException("Không tìm thấy danh mục"));

      mockMvc
          .perform(patch("/api/v1/categories/{id}/show", 99L))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục"));
    }
  }

  // ==========================================
  // 5. PATCH /api/v1/categories/{id}/display-order
  // ==========================================
  @Nested
  @DisplayName("PATCH /api/v1/categories/{id}/display-order")
  class UpdateDisplayOrderEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi cập nhật thứ tự hiển thị thành công")
    void updateDisplayOrder_ReturnsOk() throws Exception {
      when(categoryService.updateDisplayOrder(1L, 5L)).thenReturn(response(1L, "Trà sữa", true, 5L));

      mockMvc
          .perform(patch("/api/v1/categories/{id}/display-order", 1L).param("value", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.displayOrder").value(5));

      verify(categoryService, times(1)).updateDisplayOrder(1L, 5L);
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi thiếu param value")
    void updateDisplayOrder_MissingParam_ReturnsBadRequest() throws Exception {
      mockMvc
          .perform(patch("/api/v1/categories/{id}/display-order", 1L))
          .andExpect(status().isBadRequest());

      verify(categoryService, never()).updateDisplayOrder(any(), any());
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi value không phải số")
    void updateDisplayOrder_InvalidValue_ReturnsBadRequest() throws Exception {
      mockMvc
          .perform(patch("/api/v1/categories/{id}/display-order", 1L).param("value", "abc"))
          .andExpect(status().isBadRequest());

      verify(categoryService, never()).updateDisplayOrder(any(), any());
    }

    @Test
    @DisplayName("Trả về HTTP 404 Not Found khi danh mục không tồn tại")
    void updateDisplayOrder_NotFound_ReturnsNotFound() throws Exception {
      when(categoryService.updateDisplayOrder(99L, 5L))
          .thenThrow(new EntityNotFoundException("Không tìm thấy danh mục"));

      mockMvc
          .perform(patch("/api/v1/categories/{id}/display-order", 99L).param("value", "5"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục"));
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi thứ tự hiển thị không hợp lệ")
    void updateDisplayOrder_IllegalArgument_ReturnsBadRequest() throws Exception {
      when(categoryService.updateDisplayOrder(1L, -1L))
          .thenThrow(new IllegalArgumentException("Thứ tự hiển thị không hợp lệ"));

      mockMvc
          .perform(patch("/api/v1/categories/{id}/display-order", 1L).param("value", "-1"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Thứ tự hiển thị không hợp lệ"));
    }
  }

  // ==========================================
  // 6. GET /api/v1/categories/menu
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/categories/menu")
  class GetMenuEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng danh sách danh mục đang hiển thị")
    void getMenu_ReturnsOk() throws Exception {
      when(categoryService.getActiveForMenu())
          .thenReturn(List.of(
              response(1L, "Trà sữa", true, 1L),
              response(2L, "Cà phê", true, 2L)));

      mockMvc
          .perform(get("/api/v1/categories/menu"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].name").value("Trà sữa"))
          .andExpect(jsonPath("$[1].name").value("Cà phê"));

      verify(categoryService, times(1)).getActiveForMenu();
    }

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng danh sách rỗng khi không có danh mục")
    void getMenu_Empty_ReturnsEmptyList() throws Exception {
      when(categoryService.getActiveForMenu()).thenReturn(Collections.emptyList());

      mockMvc
          .perform(get("/api/v1/categories/menu"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  // ==========================================
  // 7. GET /api/v1/categories/search
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/categories/search")
  class SearchEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi tìm theo keyword và status")
    void search_WithKeywordAndStatus_ReturnsOk() throws Exception {
      when(categoryService.searchByName("trà", true))
          .thenReturn(List.of(response(1L, "Trà sữa", true, 1L)));

      mockMvc
          .perform(get("/api/v1/categories/search").param("keyword", "trà").param("status", "true"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].name").value("Trà sữa"));

      verify(categoryService, times(1)).searchByName("trà", true);
    }

    @Test
    @DisplayName("Truyền null cho service khi không có keyword và status")
    void search_WithoutParams_PassesNullToService() throws Exception {
      when(categoryService.searchByName(null, null)).thenReturn(Collections.emptyList());

      mockMvc
          .perform(get("/api/v1/categories/search"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));

      verify(categoryService, times(1)).searchByName(null, null);
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi status không phải boolean")
    void search_InvalidStatus_ReturnsBadRequest() throws Exception {
      mockMvc
          .perform(get("/api/v1/categories/search").param("status", "abc"))
          .andExpect(status().isBadRequest());

      verify(categoryService, never()).searchByName(any(), any());
    }
  }

  // ==========================================
  // 8. GET /api/v1/categories
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/categories")
  class GetAllEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng toàn bộ danh mục khi không truyền status")
    void getAll_WithoutStatus_ReturnsOk() throws Exception {
      when(categoryService.getAll(null))
          .thenReturn(List.of(
              response(1L, "Trà sữa", true, 1L),
              response(2L, "Cà phê", false, 2L)));

      mockMvc
          .perform(get("/api/v1/categories"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));

      verify(categoryService, times(1)).getAll(null);
    }

    @Test
    @DisplayName("Lọc danh mục theo status khi có truyền param")
    void getAll_WithStatus_ReturnsFiltered() throws Exception {
      when(categoryService.getAll(false))
          .thenReturn(List.of(response(2L, "Cà phê", false, 2L)));

      mockMvc
          .perform(get("/api/v1/categories").param("status", "false"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].status").value(false));

      verify(categoryService, times(1)).getAll(false);
    }
  }

  // ==========================================
  // 9. GET /api/v1/categories/{id}
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/categories/{id}")
  class GetByIdEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng thông tin danh mục")
    void getById_ReturnsOk() throws Exception {
      when(categoryService.getById(1L)).thenReturn(response(1L, "Trà sữa", true, 1L));

      mockMvc
          .perform(get("/api/v1/categories/{id}", 1L))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Trà sữa"))
          .andExpect(jsonPath("$.status").value(true))
          .andExpect(jsonPath("$.displayOrder").value(1));

      verify(categoryService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("Trả về HTTP 404 Not Found khi danh mục không tồn tại")
    void getById_NotFound_ReturnsNotFound() throws Exception {
      when(categoryService.getById(99L)).thenThrow(new EntityNotFoundException("Không tìm thấy danh mục"));

      mockMvc
          .perform(get("/api/v1/categories/{id}", 99L))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.error").value("Not Found"))
          .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi id không phải số")
    void getById_InvalidIdType_ReturnsBadRequest() throws Exception {
      mockMvc.perform(get("/api/v1/categories/{id}", "abc")).andExpect(status().isBadRequest());

      verify(categoryService, never()).getById(any());
    }
  }

  // ==========================================
  // 10. DELETE /api/v1/categories/{id}
  // ==========================================
  @Nested
  @DisplayName("DELETE /api/v1/categories/{id}")
  class DeleteEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK và thông báo khi xóa thành công")
    void delete_ReturnsOk() throws Exception {
      doNothing().when(categoryService).delete(1L);

      mockMvc
          .perform(delete("/api/v1/categories/{id}", 1L))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Xóa danh mục thành công"));

      verify(categoryService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("Trả về HTTP 404 Not Found khi xóa danh mục không tồn tại")
    void delete_NotFound_ReturnsNotFound() throws Exception {
      doThrow(new EntityNotFoundException("Không tìm thấy danh mục")).when(categoryService).delete(99L);

      mockMvc
          .perform(delete("/api/v1/categories/{id}", 99L))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục"));
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi không thể xóa danh mục")
    void delete_IllegalArgument_ReturnsBadRequest() throws Exception {
      doThrow(new IllegalArgumentException("Không thể xóa danh mục đang có sản phẩm"))
          .when(categoryService)
          .delete(1L);

      mockMvc
          .perform(delete("/api/v1/categories/{id}", 1L))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.message").value("Không thể xóa danh mục đang có sản phẩm"));
    }
  }
}