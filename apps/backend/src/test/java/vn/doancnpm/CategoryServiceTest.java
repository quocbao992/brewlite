package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.domain.Category;
import vn.doancnpm.dto.Request.CategoryRequest;
import vn.doancnpm.dto.Response.CategoryResponse;
import vn.doancnpm.repository.CategoryRepository;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.service.implement.ICategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock
  private CategoryRepository categoryRepo;

  @Mock
  private ProductRepository productRepo;

  @InjectMocks
  private ICategoryService categoryService;

  private Category category;
  private Category hiddenCategory;

  @BeforeEach
  void setUp() {
    category = Category.builder()
        .id(1L)
        .name("Trà sữa")
        .displayOrder(1L)
        .status(true)
        .build();

    hiddenCategory = Category.builder()
        .id(2L)
        .name("Cà phê")
        .displayOrder(2L)
        .status(false)
        .build();
  }

  // ==========================================
  // 1. create Tests
  // ==========================================
  @Nested
  @DisplayName("create tests")
  class CreateTests {

    @Test
    @DisplayName("Tạo danh mục thành công, tên được trim khoảng trắng")
    void create_Success_TrimsName() {
      CategoryRequest request = new CategoryRequest("  Trà sữa  ", true, 3L);

      when(categoryRepo.existsByNameIgnoreCase("Trà sữa")).thenReturn(false);
      when(categoryRepo.save(any(Category.class))).thenAnswer(inv -> {
        Category c = inv.getArgument(0);
        c.setId(1L);
        return c;
      });

      CategoryResponse response = categoryService.create(request);

      ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
      verify(categoryRepo, times(1)).save(captor.capture());
      assertThat(captor.getValue().getName()).isEqualTo("Trà sữa");
      assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3L);
      assertThat(captor.getValue().getStatus()).isTrue();

      assertThat(response.getId()).isEqualTo(1L);
      assertThat(response.getName()).isEqualTo("Trà sữa");
      assertThat(response.getDisplayOrder()).isEqualTo(3L);
      assertThat(response.getStatus()).isTrue();
    }

    @Test
    @DisplayName("Dùng giá trị mặc định displayOrder = 0 và status = true khi request để null")
    void create_UsesDefaults_WhenDisplayOrderAndStatusNull() {
      CategoryRequest request = new CategoryRequest("Trà sữa", null, null);

      when(categoryRepo.existsByNameIgnoreCase("Trà sữa")).thenReturn(false);
      when(categoryRepo.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

      CategoryResponse response = categoryService.create(request);

      assertThat(response.getDisplayOrder()).isEqualTo(0L);
      assertThat(response.getStatus()).isTrue();
    }

    @Test
    @DisplayName("Giữ nguyên status = false khi request truyền vào false")
    void create_KeepsStatusFalse_WhenRequestStatusFalse() {
      CategoryRequest request = new CategoryRequest("Trà sữa", false, 1L);

      when(categoryRepo.existsByNameIgnoreCase("Trà sữa")).thenReturn(false);
      when(categoryRepo.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

      CategoryResponse response = categoryService.create(request);

      assertThat(response.getStatus()).isFalse();
    }

    @Test
    @DisplayName("Ném exception khi tên danh mục null")
    void create_ThrowsException_WhenNameNull() {
      CategoryRequest request = new CategoryRequest(null, true, 1L);

      assertThatThrownBy(() -> categoryService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên danh mục không được để trống");

      verify(categoryRepo, never()).existsByNameIgnoreCase(any());
      verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi tên danh mục chỉ gồm khoảng trắng")
    void create_ThrowsException_WhenNameBlank() {
      CategoryRequest request = new CategoryRequest("   ", true, 1L);

      assertThatThrownBy(() -> categoryService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên danh mục không được để trống");

      verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi tên danh mục đã tồn tại")
    void create_ThrowsException_WhenNameExists() {
      CategoryRequest request = new CategoryRequest("Trà sữa", true, 1L);

      when(categoryRepo.existsByNameIgnoreCase("Trà sữa")).thenReturn(true);

      assertThatThrownBy(() -> categoryService.create(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên danh mục đã tồn tại");

      verify(categoryRepo, never()).save(any());
    }
  }

  // ==========================================
  // 2. update Tests
  // ==========================================
  @Nested
  @DisplayName("update tests")
  class UpdateTests {

    @Test
    @DisplayName("Ném exception khi không tìm thấy danh mục")
    void update_ThrowsException_WhenCategoryNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.update(99L, new CategoryRequest("A", true, 1L)))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");

      verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thành công cả tên, thứ tự hiển thị và trạng thái")
    void update_Success_AllFields() {
      CategoryRequest request = new CategoryRequest("  Cà phê sữa  ", false, 5L);

      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(categoryRepo.existsByNameIgnoreCaseAndIdNot("Cà phê sữa", 1L)).thenReturn(false);
      when(categoryRepo.save(category)).thenReturn(category);

      CategoryResponse response = categoryService.update(1L, request);

      assertThat(category.getName()).isEqualTo("Cà phê sữa");
      assertThat(category.getDisplayOrder()).isEqualTo(5L);
      assertThat(category.getStatus()).isFalse();
      assertThat(response.getName()).isEqualTo("Cà phê sữa");
      assertThat(response.getDisplayOrder()).isEqualTo(5L);
      assertThat(response.getStatus()).isFalse();
      verify(categoryRepo, times(1)).save(category);
    }

    @Test
    @DisplayName("Giữ nguyên dữ liệu cũ khi các trường trong request đều null")
    void update_KeepsOldValues_WhenRequestFieldsNull() {
      CategoryRequest request = new CategoryRequest(null, null, null);

      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(categoryRepo.save(category)).thenReturn(category);

      CategoryResponse response = categoryService.update(1L, request);

      assertThat(response.getName()).isEqualTo("Trà sữa");
      assertThat(response.getDisplayOrder()).isEqualTo(1L);
      assertThat(response.getStatus()).isTrue();
      verify(categoryRepo, never()).existsByNameIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    @DisplayName("Ném exception khi tên mới chỉ gồm khoảng trắng")
    void update_ThrowsException_WhenNameBlank() {
      CategoryRequest request = new CategoryRequest("   ", null, null);

      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

      assertThatThrownBy(() -> categoryService.update(1L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên danh mục không được để trống");

      verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi tên mới trùng với danh mục khác")
    void update_ThrowsException_WhenNameDuplicatedWithOther() {
      CategoryRequest request = new CategoryRequest("Cà phê", null, null);

      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(categoryRepo.existsByNameIgnoreCaseAndIdNot("Cà phê", 1L)).thenReturn(true);

      assertThatThrownBy(() -> categoryService.update(1L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Tên danh mục đã tồn tại");

      assertThat(category.getName()).isEqualTo("Trà sữa");
      verify(categoryRepo, never()).save(any());
    }
  }

  // ==========================================
  // 3. hide Tests
  // ==========================================
  @Nested
  @DisplayName("hide tests")
  class HideTests {

    @Test
    @DisplayName("Ẩn danh mục thành công: status chuyển sang false")
    void hide_Success() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(categoryRepo.save(category)).thenReturn(category);

      CategoryResponse response = categoryService.hide(1L);

      assertThat(category.getStatus()).isFalse();
      assertThat(response.getStatus()).isFalse();
      verify(categoryRepo, times(1)).save(category);
    }

    @Test
    @DisplayName("Ném exception khi ẩn danh mục không tồn tại")
    void hide_ThrowsException_WhenNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.hide(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");

      verify(categoryRepo, never()).save(any());
    }
  }

  // ==========================================
  // 4. show Tests
  // ==========================================
  @Nested
  @DisplayName("show tests")
  class ShowTests {

    @Test
    @DisplayName("Hiện danh mục thành công: status chuyển sang true")
    void show_Success() {
      when(categoryRepo.findById(2L)).thenReturn(Optional.of(hiddenCategory));
      when(categoryRepo.save(hiddenCategory)).thenReturn(hiddenCategory);

      CategoryResponse response = categoryService.show(2L);

      assertThat(hiddenCategory.getStatus()).isTrue();
      assertThat(response.getStatus()).isTrue();
      verify(categoryRepo, times(1)).save(hiddenCategory);
    }

    @Test
    @DisplayName("Ném exception khi hiện danh mục không tồn tại")
    void show_ThrowsException_WhenNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.show(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");

      verify(categoryRepo, never()).save(any());
    }
  }

  // ==========================================
  // 5. getById Tests
  // ==========================================
  @Nested
  @DisplayName("getById tests")
  class GetByIdTests {

    @Test
    @DisplayName("Lấy danh mục theo id thành công")
    void getById_Success() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

      CategoryResponse response = categoryService.getById(1L);

      assertThat(response.getId()).isEqualTo(1L);
      assertThat(response.getName()).isEqualTo("Trà sữa");
      assertThat(response.getStatus()).isTrue();
      assertThat(response.getDisplayOrder()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy danh mục")
    void getById_ThrowsException_WhenNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.getById(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");
    }
  }

  // ==========================================
  // 6. getAll Tests
  // ==========================================
  @Nested
  @DisplayName("getAll tests")
  class GetAllTests {

    @Test
    @DisplayName("Lấy toàn bộ danh mục theo thứ tự hiển thị khi status null")
    void getAll_NullStatus_ReturnsAllOrdered() {
      when(categoryRepo.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(category, hiddenCategory));

      List<CategoryResponse> result = categoryService.getAll(null);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getName()).isEqualTo("Trà sữa");
      assertThat(result.get(1).getName()).isEqualTo("Cà phê");
      verify(categoryRepo, never()).findByStatusOrderByDisplayOrderAsc(any());
    }

    @Test
    @DisplayName("Lọc danh mục theo status khi có truyền status")
    void getAll_WithStatus_ReturnsFiltered() {
      when(categoryRepo.findByStatusOrderByDisplayOrderAsc(false)).thenReturn(List.of(hiddenCategory));

      List<CategoryResponse> result = categoryService.getAll(false);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isFalse();
      verify(categoryRepo, never()).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi không có danh mục")
    void getAll_Empty_ReturnsEmptyList() {
      when(categoryRepo.findAllByOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());

      assertThat(categoryService.getAll(null)).isEmpty();
    }
  }

  // ==========================================
  // 7. getActiveForMenu Tests
  // ==========================================
  @Nested
  @DisplayName("getActiveForMenu tests")
  class GetActiveForMenuTests {

    @Test
    @DisplayName("Chỉ lấy các danh mục đang hiển thị (status = true)")
    void getActiveForMenu_ReturnsActiveOnly() {
      when(categoryRepo.findByStatusOrderByDisplayOrderAsc(true)).thenReturn(List.of(category));

      List<CategoryResponse> result = categoryService.getActiveForMenu();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("Trà sữa");
      verify(categoryRepo, times(1)).findByStatusOrderByDisplayOrderAsc(true);
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi không có danh mục nào đang hiển thị")
    void getActiveForMenu_Empty() {
      when(categoryRepo.findByStatusOrderByDisplayOrderAsc(true)).thenReturn(Collections.emptyList());

      assertThat(categoryService.getActiveForMenu()).isEmpty();
    }
  }

  // ==========================================
  // 8. searchByName Tests
  // ==========================================
  @Nested
  @DisplayName("searchByName tests")
  class SearchByNameTests {

    @Test
    @DisplayName("Keyword null được chuyển thành chuỗi rỗng khi status null")
    void searchByName_NullKeywordNullStatus_UsesEmptyKeyword() {
      when(categoryRepo.findByNameContainingIgnoreCaseOrderByDisplayOrderAsc(""))
          .thenReturn(List.of(category, hiddenCategory));

      List<CategoryResponse> result = categoryService.searchByName(null, null);

      assertThat(result).hasSize(2);
      verify(categoryRepo, times(1)).findByNameContainingIgnoreCaseOrderByDisplayOrderAsc("");
    }

    @Test
    @DisplayName("Keyword được trim khoảng trắng khi status null")
    void searchByName_TrimsKeyword_WhenStatusNull() {
      when(categoryRepo.findByNameContainingIgnoreCaseOrderByDisplayOrderAsc("trà"))
          .thenReturn(List.of(category));

      List<CategoryResponse> result = categoryService.searchByName("  trà  ", null);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("Trà sữa");
      verify(categoryRepo, never())
          .findByNameContainingIgnoreCaseAndStatusOrderByDisplayOrderAsc(any(), any());
    }

    @Test
    @DisplayName("Tìm theo cả keyword và status khi có truyền status")
    void searchByName_WithStatus_UsesStatusQuery() {
      when(categoryRepo.findByNameContainingIgnoreCaseAndStatusOrderByDisplayOrderAsc("cà", false))
          .thenReturn(List.of(hiddenCategory));

      List<CategoryResponse> result = categoryService.searchByName("cà", false);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("Cà phê");
      verify(categoryRepo, never()).findByNameContainingIgnoreCaseOrderByDisplayOrderAsc(any());
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi không có kết quả phù hợp")
    void searchByName_NoMatch_ReturnsEmptyList() {
      when(categoryRepo.findByNameContainingIgnoreCaseOrderByDisplayOrderAsc("xyz"))
          .thenReturn(Collections.emptyList());

      assertThat(categoryService.searchByName("xyz", null)).isEmpty();
    }
  }

  // ==========================================
  // 9. updateDisplayOrder Tests
  // ==========================================
  @Nested
  @DisplayName("updateDisplayOrder tests")
  class UpdateDisplayOrderTests {

    @Test
    @DisplayName("Ném exception khi thứ tự hiển thị null")
    void updateDisplayOrder_ThrowsException_WhenNull() {
      assertThatThrownBy(() -> categoryService.updateDisplayOrder(1L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Thứ tự hiển thị không hợp lệ");

      verify(categoryRepo, never()).findById(any());
      verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi thứ tự hiển thị là số âm")
    void updateDisplayOrder_ThrowsException_WhenNegative() {
      assertThatThrownBy(() -> categoryService.updateDisplayOrder(1L, -1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Thứ tự hiển thị không hợp lệ");

      verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy danh mục")
    void updateDisplayOrder_ThrowsException_WhenCategoryNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.updateDisplayOrder(99L, 5L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");

      verify(categoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật thứ tự hiển thị thành công")
    void updateDisplayOrder_Success() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(categoryRepo.save(category)).thenReturn(category);

      CategoryResponse response = categoryService.updateDisplayOrder(1L, 7L);

      assertThat(category.getDisplayOrder()).isEqualTo(7L);
      assertThat(response.getDisplayOrder()).isEqualTo(7L);
      verify(categoryRepo, times(1)).save(category);
    }

    @Test
    @DisplayName("Cho phép thứ tự hiển thị bằng 0 (giá trị biên)")
    void updateDisplayOrder_AllowsZero() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(categoryRepo.save(category)).thenReturn(category);

      CategoryResponse response = categoryService.updateDisplayOrder(1L, 0L);

      assertThat(response.getDisplayOrder()).isEqualTo(0L);
    }
  }

  // ==========================================
  // 10. delete Tests
  // ==========================================
  @Nested
  @DisplayName("delete tests")
  class DeleteTests {

    @Test
    @DisplayName("Xóa danh mục thành công khi không còn sản phẩm")
    void delete_Success_WhenNoProducts() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(productRepo.existsByCategoryId(1L)).thenReturn(false);

      categoryService.delete(1L);

      verify(categoryRepo, times(1)).delete(category);
    }

    @Test
    @DisplayName("Ném exception khi xóa danh mục không tồn tại")
    void delete_ThrowsException_WhenNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.delete(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");

      verify(productRepo, never()).existsByCategoryId(anyLong());
      verify(categoryRepo, never()).delete(any());
    }

    @Test
    @DisplayName("Ném exception và không xóa khi danh mục còn sản phẩm")
    void delete_ThrowsException_WhenCategoryHasProducts() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
      when(productRepo.existsByCategoryId(1L)).thenReturn(true);

      assertThatThrownBy(() -> categoryService.delete(1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Danh mục \"Trà sữa\" còn sản phẩm nên không thể xóa. "
              + "Hãy ẩn danh mục hoặc chuyển sản phẩm sang danh mục khác");

      verify(categoryRepo, never()).delete(any());
    }
  }

  // ==========================================
  // 11. existsById Tests
  // ==========================================
  @Nested
  @DisplayName("existsById tests")
  class ExistsByIdTests {

    @Test
    @DisplayName("Trả về true khi danh mục tồn tại")
    void existsById_ReturnsTrue() {
      when(categoryRepo.existsById(1L)).thenReturn(true);

      assertThat(categoryService.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("Trả về false khi danh mục không tồn tại")
    void existsById_ReturnsFalse() {
      when(categoryRepo.existsById(99L)).thenReturn(false);

      assertThat(categoryService.existsById(99L)).isFalse();
    }
  }

  // ==========================================
  // 12. isActive Tests
  // ==========================================
  @Nested
  @DisplayName("isActive tests")
  class IsActiveTests {

    @Test
    @DisplayName("Trả về true khi danh mục đang hiển thị")
    void isActive_ReturnsTrue_WhenStatusTrue() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

      assertThat(categoryService.isActive(1L)).isTrue();
    }

    @Test
    @DisplayName("Trả về false khi danh mục đang bị ẩn")
    void isActive_ReturnsFalse_WhenStatusFalse() {
      when(categoryRepo.findById(2L)).thenReturn(Optional.of(hiddenCategory));

      assertThat(categoryService.isActive(2L)).isFalse();
    }

    @Test
    @DisplayName("Trả về false khi status của danh mục là null")
    void isActive_ReturnsFalse_WhenStatusNull() {
      category.setStatus(null);
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

      assertThat(categoryService.isActive(1L)).isFalse();
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy danh mục")
    void isActive_ThrowsException_WhenNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.isActive(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");
    }
  }

  // ==========================================
  // 13. getEntityById Tests
  // ==========================================
  @Nested
  @DisplayName("getEntityById tests")
  class GetEntityByIdTests {

    @Test
    @DisplayName("Lấy entity thành công")
    void getEntityById_Success() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

      assertThat(categoryService.getEntityById(1L)).isSameAs(category);
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy danh mục")
    void getEntityById_ThrowsException_WhenNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.getEntityById(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");
    }
  }

  // ==========================================
  // 14. getActiveEntityById Tests
  // ==========================================
  @Nested
  @DisplayName("getActiveEntityById tests")
  class GetActiveEntityByIdTests {

    @Test
    @DisplayName("Lấy entity thành công khi danh mục đang hiển thị")
    void getActiveEntityById_Success_WhenActive() {
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

      assertThat(categoryService.getActiveEntityById(1L)).isSameAs(category);
    }

    @Test
    @DisplayName("Ném exception khi danh mục đang bị ẩn")
    void getActiveEntityById_ThrowsException_WhenHidden() {
      when(categoryRepo.findById(2L)).thenReturn(Optional.of(hiddenCategory));

      assertThatThrownBy(() -> categoryService.getActiveEntityById(2L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Danh mục \"Cà phê\" đang bị ẩn");
    }

    @Test
    @DisplayName("Ném exception khi status của danh mục là null")
    void getActiveEntityById_ThrowsException_WhenStatusNull() {
      category.setStatus(null);
      when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

      assertThatThrownBy(() -> categoryService.getActiveEntityById(1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Danh mục \"Trà sữa\" đang bị ẩn");
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy danh mục")
    void getActiveEntityById_ThrowsException_WhenNotFound() {
      when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.getActiveEntityById(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessage("Không tìm thấy danh mục với ID: 99");
    }
  }
}