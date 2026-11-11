package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.domain.Category;
import vn.doancnpm.domain.Product;
import vn.doancnpm.dto.Request.ProductRequest;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.dto.Response.ProductResponse;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.service.CategoryService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.service.implement.IProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ProductInventoryService productInventoryService;

  @Mock
  private CategoryService categoryService;

  @InjectMocks
  private IProductService productService;

  private Category category;
  private Category otherCategory;
  private Product product;
  private Product hiddenProduct;

  @BeforeEach
  void setUp() {
    category = Category.builder()
        .id(1L)
        .name("Trà sữa")
        .displayOrder(1L)
        .status(true)
        .build();

    otherCategory = Category.builder()
        .id(2L)
        .name("Cà phê")
        .displayOrder(2L)
        .status(true)
        .build();

    product = Product.builder()
        .id(100L)
        .category(category)
        .name("Trà sữa trân châu")
        .imgURL("https://example.com/tstc.png")
        .unitPrice(new BigDecimal("30000"))
        .status(true)
        .version(1L)
        .build();

    hiddenProduct = Product.builder()
        .id(101L)
        .category(category)
        .name("Trà sữa matcha")
        .imgURL("https://example.com/matcha.png")
        .unitPrice(new BigDecimal("35000"))
        .status(false)
        .version(1L)
        .build();

    // Tồn kho thật nằm ở ProductInventoryService, mặc định trả về 50 cho mọi sản phẩm
    lenient().when(productInventoryService.getAvailableStock(anyLong())).thenReturn(50);
  }

  // ProductRequest(name, categoryId, imgURL, unitPrice, status, stock, version)
  private ProductRequest validRequest() {
    return new ProductRequest("Trà đào", 1L, "https://example.com/tradao.png",
        new BigDecimal("25000"), true, 20, null);
  }

  // Giả lập DB gán id sau khi save
  private void stubSaveAssigningId(Long id) {
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
      Product p = inv.getArgument(0);
      p.setId(id);
      return p;
    });
  }

  // ==========================================
  // 1. create Tests
  // ==========================================
  @Nested
  @DisplayName("create tests")
  class CreateTests {

    @Test
    @DisplayName("Tạo sản phẩm thành công, tên và ảnh được trim khoảng trắng")
    void create_Success_TrimsNameAndImage() {
      ProductRequest request = new ProductRequest("  Trà đào  ", 1L, "  https://example.com/tradao.png  ",
          new BigDecimal("25000"), false, 20, null);

      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);
      when(productInventoryService.getAvailableStock(100L)).thenReturn(0);

      ProductResponse response = productService.create(request);

      ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
      verify(productRepository, times(1)).save(captor.capture());
      Product saved = captor.getValue();
      assertThat(saved.getName()).isEqualTo("Trà đào");
      assertThat(saved.getImgURL()).isEqualTo("https://example.com/tradao.png");
      assertThat(saved.getCategory()).isSameAs(category);
      assertThat(saved.getUnitPrice()).isEqualByComparingTo("25000");
      assertThat(saved.isStatus()).isFalse();

      assertThat(response.getId()).isEqualTo(100L);
      assertThat(response.getCategoryId()).isEqualTo(1L);
      assertThat(response.getCategoryName()).isEqualTo("Trà sữa");
      assertThat(response.getName()).isEqualTo("Trà đào");
      assertThat(response.getImgURL()).isEqualTo("https://example.com/tradao.png");
      assertThat(response.getStock()).isEqualTo(0);
      assertThat(response.isStatus()).isFalse();
    }

    @Test
    @DisplayName("Tạo xong thì khởi tạo dòng tồn kho = 0 với lý do INITIAL_STOCK")
    void create_InitializesInventoryWithZero() {
      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);

      productService.create(validRequest());

      verify(productInventoryService, times(1)).setStock(100L, 0, "INITIAL_STOCK");
    }

    @Test
    @DisplayName("Bỏ qua stock trong request: luôn khởi tạo tồn kho 0, không dùng giá trị 20")
    void create_IgnoresStockInRequest() {
      ProductRequest request = validRequest(); // stock = 20

      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);

      productService.create(request);

      verify(productInventoryService, times(1)).setStock(100L, 0, "INITIAL_STOCK");
      verify(productInventoryService, never()).setStock(anyLong(), eq(20), anyString());
    }

    @Test
    @DisplayName("Dùng status mặc định = true khi request để null")
    void create_UsesDefaultStatus_WhenStatusNull() {
      ProductRequest request = new ProductRequest("Trà đào", 1L, "https://example.com/tradao.png",
          new BigDecimal("25000"), null, null, null);

      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);

      ProductResponse response = productService.create(request);

      assertThat(response.isStatus()).isTrue();
    }

    @Test
    @DisplayName("Ném exception khi tên sản phẩm null")
    void create_ThrowsException_WhenNameNull() {
      ProductRequest request = validRequest();
      request.setName(null);

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên sản phẩm không được để trống");

      verify(productRepository, never()).existsByNameIgnoreCase(any());
      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi tên sản phẩm chỉ gồm khoảng trắng")
    void create_ThrowsException_WhenNameBlank() {
      ProductRequest request = validRequest();
      request.setName("   ");

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên sản phẩm không được để trống");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi tên sản phẩm dài quá 255 ký tự")
    void create_ThrowsException_WhenNameTooLong() {
      ProductRequest request = validRequest();
      request.setName("a".repeat(256));

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên sản phẩm không được dài quá 255 ký tự");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cho phép tên sản phẩm dài đúng 255 ký tự (giá trị biên)")
    void create_AllowsNameWithMaxLength() {
      ProductRequest request = validRequest();
      request.setName("a".repeat(255));

      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);

      ProductResponse response = productService.create(request);

      assertThat(response.getName()).hasSize(255);
    }

    @Test
    @DisplayName("Cho phép ảnh null, ảnh được lưu là null")
    void create_AllowsNullImage() {
      ProductRequest request = validRequest();
      request.setImgURL(null);

      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);

      ProductResponse response = productService.create(request);

      ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
      verify(productRepository).save(captor.capture());
      assertThat(captor.getValue().getImgURL()).isNull();
      assertThat(response.getImgURL()).isNull();
    }

    @Test
    @DisplayName("Ảnh chỉ gồm khoảng trắng được chuyển thành null")
    void create_BlankImage_BecomesNull() {
      ProductRequest request = validRequest();
      request.setImgURL("   ");

      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);

      productService.create(request);

      ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
      verify(productRepository).save(captor.capture());
      assertThat(captor.getValue().getImgURL()).isNull();
    }

    @Test
    @DisplayName("Ném exception khi đường dẫn ảnh dài quá 255 ký tự")
    void create_ThrowsException_WhenImageTooLong() {
      ProductRequest request = validRequest();
      request.setImgURL("a".repeat(256));

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Đường dẫn ảnh không được dài quá 255 ký tự");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi chưa chọn danh mục")
    void create_ThrowsException_WhenCategoryIdNull() {
      ProductRequest request = validRequest();
      request.setCategoryId(null);

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Phải chọn danh mục cho sản phẩm");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá null")
    void create_ThrowsException_WhenPriceNull() {
      ProductRequest request = validRequest();
      request.setUnitPrice(null);

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá bằng 0 (giá trị biên)")
    void create_ThrowsException_WhenPriceZero() {
      ProductRequest request = validRequest();
      request.setUnitPrice(BigDecimal.ZERO);

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá là số âm")
    void create_ThrowsException_WhenPriceNegative() {
      ProductRequest request = validRequest();
      request.setUnitPrice(new BigDecimal("-1000"));

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá có quá 2 chữ số thập phân")
    void create_ThrowsException_WhenPriceTooManyDecimals() {
      ProductRequest request = validRequest();
      request.setUnitPrice(new BigDecimal("100.123"));

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm chỉ được có tối đa 2 chữ số thập phân");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá vượt quá 1 tỷ")
    void create_ThrowsException_WhenPriceExceedsMax() {
      ProductRequest request = validRequest();
      request.setUnitPrice(new BigDecimal("1000000001"));

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm không được vượt quá 1.000.000.000");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cho phép giá đúng 1 tỷ (giá trị biên)")
    void create_AllowsPriceAtMax() {
      ProductRequest request = validRequest();
      request.setUnitPrice(new BigDecimal("1000000000"));

      when(categoryService.getActiveEntityById(1L)).thenReturn(category);
      stubSaveAssigningId(100L);

      ProductResponse response = productService.create(request);

      assertThat(response.getUnitPrice()).isEqualByComparingTo("1000000000");
    }

    @Test
    @DisplayName("Ném exception khi tên sản phẩm đã tồn tại")
    void create_ThrowsException_WhenNameExists() {
      ProductRequest request = validRequest();

      when(productRepository.existsByNameIgnoreCase("Trà đào")).thenReturn(true);

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên sản phẩm đã tồn tại");

      verify(categoryService, never()).getActiveEntityById(anyLong());
      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi danh mục không tồn tại")
    void create_ThrowsException_WhenCategoryNotFound() {
      ProductRequest request = validRequest();

      when(categoryService.getActiveEntityById(1L))
          .thenThrow(new EntityNotFoundException("Không tìm thấy danh mục với ID: 1"));

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 1");

      verify(productRepository, never()).save(any());
      verify(productInventoryService, never()).setStock(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Ném exception khi danh mục đang bị ẩn")
    void create_ThrowsException_WhenCategoryHidden() {
      ProductRequest request = validRequest();

      when(categoryService.getActiveEntityById(1L))
          .thenThrow(new IllegalArgumentException("Danh mục \"Trà sữa\" đang bị ẩn"));

      assertThatThrownBy(() -> productService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Danh mục \"Trà sữa\" đang bị ẩn");

      verify(productRepository, never()).save(any());
    }
  }

  // ==========================================
  // 2. update Tests
  // ==========================================
  @Nested
  @DisplayName("update tests")
  class UpdateTests {

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void update_ThrowsException_WhenProductNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.update(99L, validRequest()))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thành công tất cả trường, đổi sang danh mục khác")
    void update_Success_AllFields_ChangeCategory() {
      ProductRequest request = new ProductRequest("  Cà phê sữa  ", 2L, "  https://example.com/cfs.png  ",
          new BigDecimal("28000"), false, 15, null);

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(categoryService.getActiveEntityById(2L)).thenReturn(otherCategory);
      when(productRepository.save(product)).thenReturn(product);

      ProductResponse response = productService.update(100L, request);

      assertThat(product.getName()).isEqualTo("Cà phê sữa");
      assertThat(product.getImgURL()).isEqualTo("https://example.com/cfs.png");
      assertThat(product.getCategory()).isSameAs(otherCategory);
      assertThat(product.getUnitPrice()).isEqualByComparingTo("28000");
      assertThat(product.isStatus()).isFalse();

      assertThat(response.getCategoryId()).isEqualTo(2L);
      assertThat(response.getCategoryName()).isEqualTo("Cà phê");
      assertThat(response.getName()).isEqualTo("Cà phê sữa");
      verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Không chỉnh tồn kho khi update, kể cả khi request có gửi stock")
    void update_DoesNotTouchInventory() {
      ProductRequest request = validRequest(); // stock = 20

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.save(product)).thenReturn(product);

      ProductResponse response = productService.update(100L, request);

      verify(productInventoryService, never()).setStock(anyLong(), anyInt(), anyString());
      assertThat(response.getStock()).isEqualTo(50); // tồn kho lấy từ inventory
    }

    @Test
    @DisplayName("Không gọi categoryService khi giữ nguyên danh mục")
    void update_DoesNotLoadCategory_WhenCategoryUnchanged() {
      ProductRequest request = validRequest(); // categoryId = 1L, trùng danh mục hiện tại

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.save(product)).thenReturn(product);

      productService.update(100L, request);

      verify(categoryService, never()).getActiveEntityById(anyLong());
      assertThat(product.getCategory()).isSameAs(category);
    }

    @Test
    @DisplayName("Giữ nguyên các field cũ khi request để null hết")
    void update_KeepsOldValues_WhenRequestFieldsNull() {
      ProductRequest request = new ProductRequest(null, null, null, null, null, null, null);

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.save(product)).thenReturn(product);

      ProductResponse response = productService.update(100L, request);

      assertThat(product.getName()).isEqualTo("Trà sữa trân châu");
      assertThat(product.getImgURL()).isEqualTo("https://example.com/tstc.png");
      assertThat(product.getUnitPrice()).isEqualByComparingTo("30000");
      assertThat(product.getCategory()).isSameAs(category);
      assertThat(product.isStatus()).isTrue();
      assertThat(response.getName()).isEqualTo("Trà sữa trân châu");
      verify(categoryService, never()).getActiveEntityById(anyLong());
    }

    @Test
    @DisplayName("Ném ObjectOptimisticLockingFailureException khi version không khớp")
    void update_ThrowsException_WhenVersionMismatch() {
      ProductRequest request = validRequest();
      request.setVersion(2L); // sản phẩm đang ở version 1

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> productService.update(100L, request))
          .isInstanceOf(ObjectOptimisticLockingFailureException.class);

      assertThat(product.getName()).isEqualTo("Trà sữa trân châu");
      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thành công khi version khớp")
    void update_Success_WhenVersionMatches() {
      ProductRequest request = validRequest();
      request.setVersion(1L);

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.save(product)).thenReturn(product);

      productService.update(100L, request);

      assertThat(product.getName()).isEqualTo("Trà đào");
      verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Ném exception khi tên mới chỉ gồm khoảng trắng")
    void update_ThrowsException_WhenNameBlank() {
      ProductRequest request = validRequest();
      request.setName("   ");

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> productService.update(100L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên sản phẩm không được để trống");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá mới không hợp lệ")
    void update_ThrowsException_WhenPriceInvalid() {
      ProductRequest request = validRequest();
      request.setUnitPrice(BigDecimal.ZERO);

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> productService.update(100L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi tên mới trùng với sản phẩm khác")
    void update_ThrowsException_WhenNameDuplicatedWithOther() {
      ProductRequest request = validRequest();

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.existsByNameIgnoreCaseAndIdNot("Trà đào", 100L)).thenReturn(true);

      assertThatThrownBy(() -> productService.update(100L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên sản phẩm đã tồn tại");

      assertThat(product.getName()).isEqualTo("Trà sữa trân châu");
      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi đường dẫn ảnh mới dài quá 255 ký tự")
    void update_ThrowsException_WhenImageTooLong() {
      ProductRequest request = validRequest();
      request.setImgURL("a".repeat(256));

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> productService.update(100L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Đường dẫn ảnh không được dài quá 255 ký tự");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ảnh mới chỉ gồm khoảng trắng thì xóa ảnh (chuyển thành null)")
    void update_BlankImage_ClearsImage() {
      ProductRequest request = validRequest();
      request.setImgURL("   ");

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.save(product)).thenReturn(product);

      productService.update(100L, request);

      assertThat(product.getImgURL()).isNull();
    }

    @Test
    @DisplayName("Ném exception khi đổi sang danh mục đang bị ẩn")
    void update_ThrowsException_WhenNewCategoryHidden() {
      ProductRequest request = validRequest();
      request.setCategoryId(2L);

      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(categoryService.getActiveEntityById(2L))
          .thenThrow(new IllegalArgumentException("Danh mục \"Cà phê\" đang bị ẩn"));

      assertThatThrownBy(() -> productService.update(100L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Danh mục \"Cà phê\" đang bị ẩn");

      assertThat(product.getCategory()).isSameAs(category);
      verify(productRepository, never()).save(any());
    }
  }

  // ==========================================
  // 3. updatePrice Tests
  // ==========================================
  @Nested
  @DisplayName("updatePrice tests")
  class UpdatePriceTests {

    @Test
    @DisplayName("Cập nhật giá thành công")
    void updatePrice_Success() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.save(product)).thenReturn(product);

      ProductResponse response = productService.updatePrice(100L, new BigDecimal("45000"));

      assertThat(product.getUnitPrice()).isEqualByComparingTo("45000");
      assertThat(response.getUnitPrice()).isEqualByComparingTo("45000");
      verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Ném exception khi giá null, không truy vấn DB")
    void updatePrice_ThrowsException_WhenPriceNull() {
      assertThatThrownBy(() -> productService.updatePrice(100L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");

      verify(productRepository, never()).findById(any());
      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá bằng 0 (giá trị biên)")
    void updatePrice_ThrowsException_WhenPriceZero() {
      assertThatThrownBy(() -> productService.updatePrice(100L, BigDecimal.ZERO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá là số âm")
    void updatePrice_ThrowsException_WhenPriceNegative() {
      assertThatThrownBy(() -> productService.updatePrice(100L, new BigDecimal("-5")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm phải lớn hơn 0");

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi giá có quá 2 chữ số thập phân")
    void updatePrice_ThrowsException_WhenTooManyDecimals() {
      assertThatThrownBy(() -> productService.updatePrice(100L, new BigDecimal("10.555")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm chỉ được có tối đa 2 chữ số thập phân");

      verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Ném exception khi giá vượt quá 1 tỷ")
    void updatePrice_ThrowsException_WhenExceedsMax() {
      assertThatThrownBy(() -> productService.updatePrice(100L, new BigDecimal("1000000001")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Giá sản phẩm không được vượt quá 1.000.000.000");

      verify(productRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void updatePrice_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.updatePrice(99L, new BigDecimal("45000")))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");

      verify(productRepository, never()).save(any());
    }
  }

  // ==========================================
  // 4. updateStatus Tests
  // ==========================================
  @Nested
  @DisplayName("updateStatus tests")
  class UpdateStatusTests {

    @Test
    @DisplayName("Chuyển sản phẩm sang ngừng bán (status = false)")
    void updateStatus_Success_SetFalse() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productRepository.save(product)).thenReturn(product);

      ProductResponse response = productService.updateStatus(100L, false);

      assertThat(product.isStatus()).isFalse();
      assertThat(response.isStatus()).isFalse();
      verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Chuyển sản phẩm sang đang bán (status = true)")
    void updateStatus_Success_SetTrue() {
      when(productRepository.findById(101L)).thenReturn(Optional.of(hiddenProduct));
      when(productRepository.save(hiddenProduct)).thenReturn(hiddenProduct);

      ProductResponse response = productService.updateStatus(101L, true);

      assertThat(hiddenProduct.isStatus()).isTrue();
      assertThat(response.isStatus()).isTrue();
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void updateStatus_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.updateStatus(99L, true))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");

      verify(productRepository, never()).save(any());
    }
  }

  // ==========================================
  // 5. updateStock Tests
  // ==========================================
  @Nested
  @DisplayName("updateStock tests")
  class UpdateStockTests {

    @Test
    @DisplayName("Cập nhật tồn kho thành công qua ProductInventoryService, không save Product")
    void updateStock_Success() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productInventoryService.getAvailableStock(100L)).thenReturn(80);

      ProductResponse response = productService.updateStock(100L, 80);

      verify(productInventoryService, times(1)).setStock(100L, 80, "ADMIN_ADJUST");
      verify(productRepository, never()).save(any());
      assertThat(response.getStock()).isEqualTo(80);
    }

    @Test
    @DisplayName("Cho phép tồn kho bằng 0 (giá trị biên)")
    void updateStock_AllowsZero() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productInventoryService.getAvailableStock(100L)).thenReturn(0);

      ProductResponse response = productService.updateStock(100L, 0);

      verify(productInventoryService, times(1)).setStock(100L, 0, "ADMIN_ADJUST");
      assertThat(response.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("Ném exception khi tồn kho là số âm, không truy vấn DB")
    void updateStock_ThrowsException_WhenNegative() {
      assertThatThrownBy(() -> productService.updateStock(100L, -1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số lượng tồn kho không được âm");

      verify(productRepository, never()).findById(any());
      verify(productInventoryService, never()).setStock(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void updateStock_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.updateStock(99L, 10))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");

      verify(productInventoryService, never()).setStock(anyLong(), anyInt(), anyString());
    }
  }

  // ==========================================
  // 6. delete Tests
  // ==========================================
  @Nested
  @DisplayName("delete tests")
  class DeleteTests {

    @Test
    @DisplayName("Xóa mềm thành công: status chuyển sang false, không xóa khỏi DB")
    void delete_Success_SoftDelete() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      productService.delete(100L);

      assertThat(product.isStatus()).isFalse();
      verify(productRepository, times(1)).save(product);
      verify(productRepository, never()).delete(any());
      verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Ném exception khi xóa sản phẩm không tồn tại")
    void delete_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.delete(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");

      verify(productRepository, never()).save(any());
    }
  }

  // ==========================================
  // 7. getById Tests
  // ==========================================
  @Nested
  @DisplayName("getById tests")
  class GetByIdTests {

    @Test
    @DisplayName("Lấy sản phẩm theo id thành công")
    void getById_Success() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      ProductResponse response = productService.getById(100L);

      assertThat(response.getId()).isEqualTo(100L);
      assertThat(response.getCategoryId()).isEqualTo(1L);
      assertThat(response.getCategoryName()).isEqualTo("Trà sữa");
      assertThat(response.getName()).isEqualTo("Trà sữa trân châu");
      assertThat(response.getImgURL()).isEqualTo("https://example.com/tstc.png");
      assertThat(response.getStock()).isEqualTo(50);
      assertThat(response.getUnitPrice()).isEqualByComparingTo("30000");
      assertThat(response.isStatus()).isTrue();
      assertThat(response.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Tồn kho trong response lấy từ ProductInventoryService")
    void getById_ReadsStockFromInventoryService() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(productInventoryService.getAvailableStock(100L)).thenReturn(7);

      ProductResponse response = productService.getById(100L);

      assertThat(response.getStock()).isEqualTo(7);
      verify(productInventoryService, times(1)).getAvailableStock(100L);
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void getById_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.getById(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 8. search Tests
  // ==========================================
  @Nested
  @DisplayName("search tests")
  class SearchTests {

    @Test
    @DisplayName("Tìm kiếm thành công và map kết quả sang PageResponse")
    void search_Success() {
      when(productRepository.search(eq(1L), eq("trà"), eq(true), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));

      PageResponse<ProductResponse> result = productService.search(1L, "trà", true, 0, 10);

      assertThat(result).isNotNull();
      assertThat(result).extracting("content", InstanceOfAssertFactories.LIST).hasSize(1);
      verify(productRepository, times(1)).search(eq(1L), eq("trà"), eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("Keyword null hoặc chỉ gồm khoảng trắng được chuyển thành null")
    void search_BlankKeyword_BecomesNull() {
      when(productRepository.search(eq(null), eq(null), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      productService.search(null, "   ", null, 0, 10);
      productService.search(null, null, null, 0, 10);

      verify(productRepository, times(2)).search(eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("Keyword được trim khoảng trắng")
    void search_TrimsKeyword() {
      when(productRepository.search(eq(null), eq("trà"), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(product)));

      productService.search(null, "  trà  ", null, 0, 10);

      verify(productRepository, times(1)).search(eq(null), eq("trà"), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("Ký tự đặc biệt % và _ trong keyword được escape")
    void search_EscapesWildcardCharacters() {
      when(productRepository.search(eq(null), eq("50\\%\\_off"), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      productService.search(null, "50%_off", null, 0, 10);

      verify(productRepository, times(1)).search(eq(null), eq("50\\%\\_off"), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("Page âm được chuyển về 0")
    void search_NegativePage_BecomesZero() {
      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      when(productRepository.search(any(), any(), any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      productService.search(null, null, null, -5, 10);

      verify(productRepository).search(any(), any(), any(), captor.capture());
      assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
      assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Size <= 0 được chuyển về mặc định 10")
    void search_NonPositiveSize_UsesDefault() {
      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      when(productRepository.search(any(), any(), any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      productService.search(null, null, null, 0, 0);

      verify(productRepository).search(any(), any(), any(), captor.capture());
      assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Size > 100 bị giới hạn ở 100")
    void search_SizeTooLarge_CappedAtMax() {
      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      when(productRepository.search(any(), any(), any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      productService.search(null, null, null, 2, 1000);

      verify(productRepository).search(any(), any(), any(), captor.capture());
      assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
      assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Sắp xếp theo id tăng dần")
    void search_SortsByIdAscending() {
      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      when(productRepository.search(any(), any(), any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      productService.search(null, null, null, 0, 10);

      verify(productRepository).search(any(), any(), any(), captor.capture());
      assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
      assertThat(captor.getValue().getSort().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    @DisplayName("Trả về trang rỗng khi không có kết quả")
    void search_NoResult_ReturnsEmptyContent() {
      when(productRepository.search(any(), any(), any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      PageResponse<ProductResponse> result = productService.search(null, "xyz", null, 0, 10);

      assertThat(result).extracting("content", InstanceOfAssertFactories.LIST).isEmpty();
    }
  }

  // ==========================================
  // 9. getByCategory Tests
  // ==========================================
  @Nested
  @DisplayName("getByCategory tests")
  class GetByCategoryTests {

    @Test
    @DisplayName("Ném exception khi danh mục không tồn tại")
    void getByCategory_ThrowsException_WhenCategoryNotFound() {
      when(categoryService.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> productService.getByCategory(99L, true))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");

      verify(productRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lấy toàn bộ sản phẩm của danh mục, sắp xếp theo tên")
    void getByCategory_Success() {
      when(categoryService.existsById(1L)).thenReturn(true);
      when(productRepository.search(eq(1L), eq(null), eq(true), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(product)));

      List<ProductResponse> result = productService.getByCategory(1L, true);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("Trà sữa trân châu");

      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(productRepository).search(eq(1L), eq(null), eq(true), captor.capture());
      assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
      assertThat(captor.getValue().getPageSize()).isEqualTo(Integer.MAX_VALUE);
      assertThat(captor.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
    }

    @Test
    @DisplayName("Truyền status null để lấy cả sản phẩm đang bán và ngừng bán")
    void getByCategory_NullStatus_ReturnsAll() {
      when(categoryService.existsById(1L)).thenReturn(true);
      when(productRepository.search(eq(1L), eq(null), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(product, hiddenProduct)));

      List<ProductResponse> result = productService.getByCategory(1L, null);

      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi danh mục chưa có sản phẩm")
    void getByCategory_Empty() {
      when(categoryService.existsById(1L)).thenReturn(true);
      when(productRepository.search(eq(1L), eq(null), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      assertThat(productService.getByCategory(1L, null)).isEmpty();
    }
  }

  // ==========================================
  // 10. getFeatured Tests
  // ==========================================
  @Nested
  @DisplayName("getFeatured tests")
  class GetFeaturedTests {

    @Test
    @DisplayName("Lấy sản phẩm nổi bật theo limit truyền vào")
    void getFeatured_Success() {
      when(productRepository.findByStatusOrderByIdDesc(eq(true), any(Pageable.class)))
          .thenReturn(List.of(product));

      List<ProductResponse> result = productService.getFeatured(3);

      assertThat(result).hasSize(1);
      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(productRepository).findByStatusOrderByIdDesc(eq(true), captor.capture());
      assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
      assertThat(captor.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("Limit <= 0 được chuyển về mặc định 6")
    void getFeatured_NonPositiveLimit_UsesDefault() {
      when(productRepository.findByStatusOrderByIdDesc(eq(true), any(Pageable.class)))
          .thenReturn(Collections.emptyList());

      productService.getFeatured(0);

      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(productRepository).findByStatusOrderByIdDesc(eq(true), captor.capture());
      assertThat(captor.getValue().getPageSize()).isEqualTo(6);
    }

    @Test
    @DisplayName("Limit > 50 bị giới hạn ở 50")
    void getFeatured_LimitTooLarge_CappedAtMax() {
      when(productRepository.findByStatusOrderByIdDesc(eq(true), any(Pageable.class)))
          .thenReturn(Collections.emptyList());

      productService.getFeatured(500);

      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(productRepository).findByStatusOrderByIdDesc(eq(true), captor.capture());
      assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }
  }

  // ==========================================
  // 11. getMenu Tests
  // ==========================================
  @Nested
  @DisplayName("getMenu tests")
  class GetMenuTests {

    @Test
    @DisplayName("Lấy menu thành công")
    void getMenu_Success() {
      when(productRepository.findMenu()).thenReturn(List.of(product));

      List<ProductResponse> result = productService.getMenu();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("Trà sữa trân châu");
      verify(productRepository, times(1)).findMenu();
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi menu trống")
    void getMenu_Empty() {
      when(productRepository.findMenu()).thenReturn(Collections.emptyList());

      assertThat(productService.getMenu()).isEmpty();
    }
  }

  // ==========================================
  // 12. existsById Tests
  // ==========================================
  @Nested
  @DisplayName("existsById tests")
  class ExistsByIdTests {

    @Test
    @DisplayName("Trả về true khi sản phẩm tồn tại")
    void existsById_ReturnsTrue() {
      when(productRepository.existsById(100L)).thenReturn(true);

      assertThat(productService.existsById(100L)).isTrue();
    }

    @Test
    @DisplayName("Trả về false khi sản phẩm không tồn tại")
    void existsById_ReturnsFalse() {
      when(productRepository.existsById(99L)).thenReturn(false);

      assertThat(productService.existsById(99L)).isFalse();
    }
  }

  // ==========================================
  // 13. isActive Tests
  // ==========================================
  @Nested
  @DisplayName("isActive tests")
  class IsActiveTests {

    @Test
    @DisplayName("Trả về true khi sản phẩm đang bán")
    void isActive_ReturnsTrue() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      assertThat(productService.isActive(100L)).isTrue();
    }

    @Test
    @DisplayName("Trả về false khi sản phẩm đã ngừng bán")
    void isActive_ReturnsFalse() {
      when(productRepository.findById(101L)).thenReturn(Optional.of(hiddenProduct));

      assertThat(productService.isActive(101L)).isFalse();
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void isActive_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.isActive(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 14. getEntityById Tests
  // ==========================================
  @Nested
  @DisplayName("getEntityById tests")
  class GetEntityByIdTests {

    @Test
    @DisplayName("Lấy entity thành công, kể cả khi sản phẩm đã ngừng bán")
    void getEntityById_Success() {
      when(productRepository.findById(101L)).thenReturn(Optional.of(hiddenProduct));

      assertThat(productService.getEntityById(101L)).isSameAs(hiddenProduct);
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void getEntityById_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.getEntityById(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 15. getActiveEntityById Tests
  // ==========================================
  @Nested
  @DisplayName("getActiveEntityById tests")
  class GetActiveEntityByIdTests {

    @Test
    @DisplayName("Lấy entity thành công khi sản phẩm đang bán")
    void getActiveEntityById_Success() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      assertThat(productService.getActiveEntityById(100L)).isSameAs(product);
    }

    @Test
    @DisplayName("Ném exception khi sản phẩm đã ngừng bán")
    void getActiveEntityById_ThrowsException_WhenInactive() {
      when(productRepository.findById(101L)).thenReturn(Optional.of(hiddenProduct));

      assertThatThrownBy(() -> productService.getActiveEntityById(101L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Sản phẩm \"Trà sữa matcha\" đã ngừng bán");
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void getActiveEntityById_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.getActiveEntityById(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }

  // ==========================================
  // 16. getUnitPrice Tests
  // ==========================================
  @Nested
  @DisplayName("getUnitPrice tests")
  class GetUnitPriceTests {

    @Test
    @DisplayName("Lấy đơn giá thành công khi sản phẩm đang bán")
    void getUnitPrice_Success() {
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));

      assertThat(productService.getUnitPrice(100L)).isEqualByComparingTo("30000");
    }
    

    @Test
    @DisplayName("Ném exception khi sản phẩm đã ngừng bán")
    void getUnitPrice_ThrowsException_WhenInactive() {
      when(productRepository.findById(101L)).thenReturn(Optional.of(hiddenProduct));

      assertThatThrownBy(() -> productService.getUnitPrice(101L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Sản phẩm \"Trà sữa matcha\" đã ngừng bán");
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void getUnitPrice_ThrowsException_WhenNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> productService.getUnitPrice(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy sản phẩm với ID: 99");
    }
  }
}