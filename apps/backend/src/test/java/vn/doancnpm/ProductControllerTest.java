package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.controller.ProductController;
import vn.doancnpm.domain.Product;
import vn.doancnpm.dto.Request.ProductRequest;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.dto.Response.ProductResponse;
import vn.doancnpm.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Mock
  private ProductService productService;

  @InjectMocks
  private ProductController productController;

  private ProductResponse productResponse;
  private ProductResponse otherResponse;

  @BeforeEach
  void setUp() {
    // ProductResponse(id, categoryId, categoryName, name, imgURL, stock, unitPrice, status, version)
    productResponse = new ProductResponse(100L, 1L, "Trà sữa", "Trà sữa trân châu",
        "https://example.com/tstc.png", 50, new BigDecimal("30000"), true, 1L);

    otherResponse = new ProductResponse(101L, 1L, "Trà sữa", "Trà sữa matcha",
        "https://example.com/matcha.png", 10, new BigDecimal("35000"), false, 1L);
  }

  // ProductRequest(name, categoryId, imgURL, unitPrice, status, stock, version)
  private ProductRequest validRequest() {
    return new ProductRequest("Trà đào", 1L, "https://example.com/tradao.png",
        new BigDecimal("25000"), true, 20, null);
  }

  // ==========================================
  // 1. search Tests
  // ==========================================
  @Nested
  @DisplayName("search tests")
  class SearchTests {

    @Test
    @DisplayName("Tìm kiếm trả về 200 và truyền đúng tham số xuống service")
    @SuppressWarnings("unchecked")
    void search_Success() {
      PageResponse<ProductResponse> page = mock(PageResponse.class);
      when(productService.search(1L, "trà", true, 0, 10)).thenReturn(page);

      ResponseEntity<PageResponse<ProductResponse>> response = productController.search(1L, "trà", true, 0, 10);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(page);
      verify(productService, times(1)).search(1L, "trà", true, 0, 10);
    }

    @Test
    @DisplayName("Các tham số lọc để null vẫn được chuyển nguyên xuống service")
    @SuppressWarnings("unchecked")
    void search_NullFilters_PassedThrough() {
      PageResponse<ProductResponse> page = mock(PageResponse.class);
      when(productService.search(null, null, null, 0, 10)).thenReturn(page);

      ResponseEntity<PageResponse<ProductResponse>> response = productController.search(null, null, null, 0, 10);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(page);
    }
  }

  // ==========================================
  // 2. getByCategory Tests
  // ==========================================
  @Nested
  @DisplayName("getByCategory tests")
  class GetByCategoryTests {

    @Test
    @DisplayName("Lấy sản phẩm theo danh mục trả về 200")
    void getByCategory_Success() {
      when(productService.getByCategory(1L, true)).thenReturn(List.of(productResponse));

      ResponseEntity<List<ProductResponse>> response = productController.getByCategory(1L, true);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).containsExactly(productResponse);
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi danh mục chưa có sản phẩm")
    void getByCategory_Empty() {
      when(productService.getByCategory(1L, null)).thenReturn(Collections.emptyList());

      ResponseEntity<List<ProductResponse>> response = productController.getByCategory(1L, null);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp để @ExceptionHandler xử lý")
    void getByCategory_PropagatesException_WhenCategoryNotFound() {
      when(productService.getByCategory(99L, null))
          .thenThrow(new EntityNotFoundException("Không tìm thấy danh mục với ID: 99"));

      assertThatThrownBy(() -> productController.getByCategory(99L, null))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");
    }
  }

  // ==========================================
  // 3. getFeatured Tests
  // ==========================================
  @Nested
  @DisplayName("getFeatured tests")
  class GetFeaturedTests {

    @Test
    @DisplayName("Lấy sản phẩm nổi bật trả về 200 theo limit truyền vào")
    void getFeatured_Success() {
      when(productService.getFeatured(3)).thenReturn(List.of(productResponse));

      ResponseEntity<List<ProductResponse>> response = productController.getFeatured(3);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).hasSize(1);
      verify(productService, times(1)).getFeatured(3);
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi không có sản phẩm nổi bật")
    void getFeatured_Empty() {
      when(productService.getFeatured(6)).thenReturn(Collections.emptyList());

      assertThat(productController.getFeatured(6).getBody()).isEmpty();
    }
  }

  // ==========================================
  // 4. getMenu Tests
  // ==========================================
  @Nested
  @DisplayName("getMenu tests")
  class GetMenuTests {

    @Test
    @DisplayName("Lấy menu trả về 200")
    void getMenu_Success() {
      when(productService.getMenu()).thenReturn(List.of(productResponse, otherResponse));

      ResponseEntity<List<ProductResponse>> response = productController.getMenu();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).hasSize(2);
      verify(productService, times(1)).getMenu();
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi menu trống")
    void getMenu_Empty() {
      when(productService.getMenu()).thenReturn(Collections.emptyList());

      assertThat(productController.getMenu().getBody()).isEmpty();
    }
  }

  // ==========================================
  // 5. getById Tests
  // ==========================================
  @Nested
  @DisplayName("getById tests")
  class GetByIdTests {

    @Test
    @DisplayName("Lấy sản phẩm theo id trả về 200")
    void getById_Success() {
      when(productService.getById(100L)).thenReturn(productResponse);

      ResponseEntity<ProductResponse> response = productController.getById(100L);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(productResponse);
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp khi không tìm thấy sản phẩm")
    void getById_PropagatesException_WhenNotFound() {
      when(productService.getById(99L))
          .thenThrow(new EntityNotFoundException("Không tìm thấy sản phẩm với ID: 99"));

      assertThatThrownBy(() -> productController.getById(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 6. create Tests
  // ==========================================
  @Nested
  @DisplayName("create tests")
  class CreateTests {

    @Test
    @DisplayName("Tạo sản phẩm trả về 201 và body là sản phẩm vừa tạo")
    void create_Success_Returns201() {
      ProductRequest request = validRequest();
      when(productService.create(request)).thenReturn(productResponse);

      ResponseEntity<ProductResponse> response = productController.create(request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isSameAs(productResponse);
      verify(productService, times(1)).create(request);
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp khi dữ liệu không hợp lệ")
    void create_PropagatesException_WhenInvalid() {
      ProductRequest request = validRequest();
      when(productService.create(request))
          .thenThrow(new IllegalArgumentException("Tên sản phẩm đã tồn tại"));

      assertThatThrownBy(() -> productController.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên sản phẩm đã tồn tại");
    }
  }

  // ==========================================
  // 7. update Tests
  // ==========================================
  @Nested
  @DisplayName("update tests")
  class UpdateTests {

    @Test
    @DisplayName("Cập nhật sản phẩm trả về 200")
    void update_Success() {
      ProductRequest request = validRequest();
      when(productService.update(100L, request)).thenReturn(productResponse);

      ResponseEntity<ProductResponse> response = productController.update(100L, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(productResponse);
      verify(productService, times(1)).update(100L, request);
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp khi không tìm thấy sản phẩm")
    void update_PropagatesException_WhenNotFound() {
      ProductRequest request = validRequest();
      when(productService.update(99L, request))
          .thenThrow(new EntityNotFoundException("Không tìm thấy sản phẩm với ID: 99"));

      assertThatThrownBy(() -> productController.update(99L, request))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp khi version không khớp")
    void update_PropagatesException_WhenVersionMismatch() {
      ProductRequest request = validRequest();
      when(productService.update(100L, request))
          .thenThrow(new ObjectOptimisticLockingFailureException(Product.class, 100L));

      assertThatThrownBy(() -> productController.update(100L, request))
          .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
  }

  // ==========================================
  // 8. updatePrice Tests
  // ==========================================
  @Nested
  @DisplayName("updatePrice tests")
  class UpdatePriceTests {

    @Test
    @DisplayName("Cập nhật giá trả về 200")
    void updatePrice_Success() {
      BigDecimal newPrice = new BigDecimal("45000");
      when(productService.updatePrice(100L, newPrice)).thenReturn(productResponse);

      ResponseEntity<ProductResponse> response = productController.updatePrice(100L, newPrice);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(productResponse);
      verify(productService, times(1)).updatePrice(100L, newPrice);
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp khi giá không hợp lệ")
    void updatePrice_PropagatesException_WhenInvalidPrice() {
      BigDecimal badPrice = BigDecimal.ZERO;
      when(productService.updatePrice(100L, badPrice))
          .thenThrow(new IllegalArgumentException("Giá sản phẩm phải lớn hơn 0"));

      assertThatThrownBy(() -> productController.updatePrice(100L, badPrice))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");
    }
  }

  // ==========================================
  // 9. updateStatus Tests
  // ==========================================
  @Nested
  @DisplayName("updateStatus tests")
  class UpdateStatusTests {

    @Test
    @DisplayName("Chuyển sang ngừng bán trả về 200")
    void updateStatus_Success_SetFalse() {
      when(productService.updateStatus(100L, false)).thenReturn(otherResponse);

      ResponseEntity<ProductResponse> response = productController.updateStatus(100L, false);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(otherResponse);
      verify(productService, times(1)).updateStatus(100L, false);
    }

    @Test
    @DisplayName("Chuyển sang đang bán trả về 200")
    void updateStatus_Success_SetTrue() {
      when(productService.updateStatus(101L, true)).thenReturn(productResponse);

      ResponseEntity<ProductResponse> response = productController.updateStatus(101L, true);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isSameAs(productResponse);
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp khi không tìm thấy sản phẩm")
    void updateStatus_PropagatesException_WhenNotFound() {
      when(productService.updateStatus(99L, true))
          .thenThrow(new EntityNotFoundException("Không tìm thấy sản phẩm với ID: 99"));

      assertThatThrownBy(() -> productController.updateStatus(99L, true))
          .isInstanceOf(EntityNotFoundException.class);
    }
  }

  // ==========================================
  // 10. delete Tests
  // ==========================================
  @Nested
  @DisplayName("delete tests")
  class DeleteTests {

    @Test
    @DisplayName("Xóa thành công trả về 200 kèm thông báo")
    void delete_Success() {
      ResponseEntity<Map<String, String>> response = productController.delete(100L);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).containsEntry("message", "Xóa sản phẩm thành công");
      verify(productService, times(1)).delete(100L);
    }

    @Test
    @DisplayName("Exception từ service được ném tiếp khi không tìm thấy sản phẩm")
    void delete_PropagatesException_WhenNotFound() {
      org.mockito.Mockito.doThrow(new EntityNotFoundException("Không tìm thấy sản phẩm với ID: 99"))
          .when(productService).delete(99L);

      assertThatThrownBy(() -> productController.delete(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 11. Exception handler Tests
  // ==========================================
  @Nested
  @DisplayName("exception handler tests")
  class ExceptionHandlerTests {

    private void assertErrorBody(ResponseEntity<Map<String, Object>> response,
        HttpStatus expectedStatus, String expectedMessage) {
      assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
      Map<String, Object> body = response.getBody();
      assertThat(body).isNotNull();
      assertThat(body.get("status")).isEqualTo(expectedStatus.value());
      assertThat(body.get("error")).isEqualTo(expectedStatus.getReasonPhrase());
      assertThat(body.get("message")).isEqualTo(expectedMessage);
      assertThat(body.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("EntityNotFoundException trả về 404 với message của exception")
    void handleNotFound_Returns404() {
      ResponseEntity<Map<String, Object>> response = productController
          .handleNotFound(new EntityNotFoundException("Không tìm thấy sản phẩm với ID: 99"));

      assertErrorBody(response, HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: 99");
    }

    @Test
    @DisplayName("IllegalArgumentException trả về 400 với message của exception")
    void handleBadRequest_Returns400() {
      ResponseEntity<Map<String, Object>> response = productController
          .handleBadRequest(new IllegalArgumentException("Tên sản phẩm không được để trống"));

      assertErrorBody(response, HttpStatus.BAD_REQUEST, "Tên sản phẩm không được để trống");
    }

    @Test
    @DisplayName("Sai kiểu tham số trả về 400 kèm tên và giá trị tham số")
    void handleTypeMismatch_Returns400() {
      MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
      when(ex.getName()).thenReturn("id");
      when(ex.getValue()).thenReturn("abc");

      ResponseEntity<Map<String, Object>> response = productController.handleTypeMismatch(ex);

      assertErrorBody(response, HttpStatus.BAD_REQUEST, "Tham số 'id' có giá trị không hợp lệ: abc");
    }

    @Test
    @DisplayName("Thiếu tham số bắt buộc trả về 400 kèm tên tham số")
    void handleMissingParam_Returns400() {
      MissingServletRequestParameterException ex = new MissingServletRequestParameterException("price",
          "BigDecimal");

      ResponseEntity<Map<String, Object>> response = productController.handleMissingParam(ex);

      assertErrorBody(response, HttpStatus.BAD_REQUEST, "Thiếu tham số: price");
    }

    @Test
    @DisplayName("Body JSON sai định dạng trả về 400")
    void handleBadJson_Returns400() {
      HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

      ResponseEntity<Map<String, Object>> response = productController.handleBadJson(ex);

      assertErrorBody(response, HttpStatus.BAD_REQUEST, "Body JSON không hợp lệ");
    }

    @Test
    @DisplayName("Xung đột version (optimistic lock) trả về 409")
    void handleConflict_Returns409() {
      ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(Product.class,
          100L);

      ResponseEntity<Map<String, Object>> response = productController.handleConflict(ex);

      assertErrorBody(response, HttpStatus.CONFLICT,
          "Sản phẩm vừa được người khác cập nhật, vui lòng tải lại và thử lại");
    }

    @Test
    @DisplayName("Exception không lường trước trả về 500 với message của exception")
    void handleAllExceptions_Returns500() {
      ResponseEntity<Map<String, Object>> response = productController
          .handleAllExceptions(new RuntimeException("Lỗi bất ngờ"));

      assertErrorBody(response, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi bất ngờ");
    }

    @Test
    @DisplayName("Handler không gọi xuống service")
    void handlers_DoNotTouchService() {
      productController.handleNotFound(new EntityNotFoundException("x"));
      productController.handleBadRequest(new IllegalArgumentException("y"));

      verify(productService, never()).getById(org.mockito.ArgumentMatchers.anyLong());
    }
  }
}