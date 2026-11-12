package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

import vn.doancnpm.domain.InventoryTopping;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.dto.Request.ToppingRequest;
import vn.doancnpm.dto.Response.ToppingResponse;
import vn.doancnpm.repository.InventoryToppingRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.service.implement.IToppingService;


@ExtendWith(MockitoExtension.class)
class ToppingServiceTest {

  @Mock
  private ToppingRepository toppingRepository;

  @Mock
  private InventoryToppingRepository inventoryToppingRepository;

  @InjectMocks
  private IToppingService service;

  private Topping tranChau;
  private Topping thachDua;

  @BeforeEach
  void setUp() {
    tranChau = Topping.builder()
        .id(1L)
        .name("Trân châu")
        .unitPrice(new BigDecimal("5000"))
        .status(true)
        .build();

    thachDua = Topping.builder()
        .id(2L)
        .name("Thạch dừa")
        .unitPrice(new BigDecimal("4000"))
        .status(false)
        .build();
  }

  private InventoryTopping inventory(Topping t, int stock) {
    return InventoryTopping.builder().id(t.getId()).topping(t).stock(stock).minThreshold(10).build();
  }

  private ToppingRequest request(String name, String price) {
    return ToppingRequest.builder()
        .name(name)
        .unitPrice(price == null ? null : new BigDecimal(price))
        .build();
  }

  
  private void stubSaveNewTopping() {
    when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> {
      Topping t = invocation.getArgument(0);
      t.setId(7L);
      return t;
    });
  }

  // ==========================================
  // 1. getAllToppings Tests
  // ==========================================
  @Nested
  @DisplayName("getAllToppings tests")
  class GetAllToppingsTests {

    @Test
    @DisplayName("status = null: lấy tất cả topping và map tồn kho")
    void getAllToppings_StatusNull_ReturnsAllAndMapsStock() {
      when(toppingRepository.findAll()).thenReturn(List.of(tranChau, thachDua));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(tranChau, 15)));
      when(inventoryToppingRepository.findByToppingId(2L)).thenReturn(Optional.empty());

      List<ToppingResponse> result = service.getAllToppings(null);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getName()).isEqualTo("Trân châu");
      assertThat(result.get(0).getStock()).isEqualTo(15);
      assertThat(result.get(0).getStatus()).isTrue();
      assertThat(result.get(1).getName()).isEqualTo("Thạch dừa");
      assertThat(result.get(1).getStock()).isZero(); // chưa có dòng tồn kho -> 0
      assertThat(result.get(1).getStatus()).isFalse();
      verify(toppingRepository, never()).findByStatus(true);
      verify(toppingRepository, never()).findByStatus(false);
    }

    @Test
    @DisplayName("status = true: chỉ lấy topping đang bán")
    void getAllToppings_StatusTrue_FiltersByStatus() {
      when(toppingRepository.findByStatus(true)).thenReturn(List.of(tranChau));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(tranChau, 8)));

      List<ToppingResponse> result = service.getAllToppings(true);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getId()).isEqualTo(1L);
      assertThat(result.get(0).getStock()).isEqualTo(8);
      verify(toppingRepository, never()).findAll();
      verify(toppingRepository, never()).findByStatus(false);
    }

    @Test
    @DisplayName("status = false: chỉ lấy topping ngừng bán")
    void getAllToppings_StatusFalse_FiltersByStatus() {
      when(toppingRepository.findByStatus(false)).thenReturn(List.of(thachDua));
      when(inventoryToppingRepository.findByToppingId(2L)).thenReturn(Optional.empty());

      List<ToppingResponse> result = service.getAllToppings(false);

      assertThat(result).extracting(ToppingResponse::getId).containsExactly(2L);
      verify(toppingRepository, never()).findAll();
      verify(toppingRepository, never()).findByStatus(true);
    }

    @Test
    @DisplayName("Không có dữ liệu: trả về danh sách rỗng và không tra tồn kho")
    void getAllToppings_NoData_ReturnsEmptyList() {
      when(toppingRepository.findAll()).thenReturn(Collections.emptyList());

      assertThat(service.getAllToppings(null)).isEmpty();
      verifyNoInteractions(inventoryToppingRepository);
    }

    @Test
    @DisplayName("Lọc theo status nhưng không có topping khớp: trả về danh sách rỗng")
    void getAllToppings_FilterNoMatch_ReturnsEmptyList() {
      when(toppingRepository.findByStatus(true)).thenReturn(Collections.emptyList());

      assertThat(service.getAllToppings(true)).isEmpty();
      verifyNoInteractions(inventoryToppingRepository);
    }

    @Test
    @DisplayName("Giữ nguyên thứ tự topping do repository trả về")
    void getAllToppings_KeepsRepositoryOrder() {
      when(toppingRepository.findAll()).thenReturn(List.of(thachDua, tranChau));
      when(inventoryToppingRepository.findByToppingId(2L)).thenReturn(Optional.empty());
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(tranChau, 3)));

      List<ToppingResponse> result = service.getAllToppings(null);

      assertThat(result).extracting(ToppingResponse::getId).containsExactly(2L, 1L);
    }
  }

  // ==========================================
  // 2. getToppingById Tests
  // ==========================================
  @Nested
  @DisplayName("getToppingById tests")
  class GetToppingByIdTests {

    @Test
    @DisplayName("Trả về response đầy đủ khi topping tồn tại")
    void getToppingById_Success_ReturnsFullResponse() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(tranChau));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(tranChau, 15)));

      ToppingResponse response = service.getToppingById(1L);

      assertThat(response.getId()).isEqualTo(1L);
      assertThat(response.getName()).isEqualTo("Trân châu");
      assertThat(response.getUnitPrice()).isEqualByComparingTo("5000");
      assertThat(response.getStock()).isEqualTo(15);
      assertThat(response.getStatus()).isTrue();
    }

    @Test
    @DisplayName("Chưa có dòng tồn kho thì stock = 0")
    void getToppingById_StockIsZero_WhenInventoryRowMissing() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(tranChau));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      ToppingResponse response = service.getToppingById(1L);

      assertThat(response.getStock()).isZero();
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy topping")
    void getToppingById_ThrowsException_WhenNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      // isInstanceOf(RuntimeException) vẫn đúng khi đổi sang EntityNotFoundException
      assertThatThrownBy(() -> service.getToppingById(99L))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("99");

      verifyNoInteractions(inventoryToppingRepository);
    }
  }

  // ==========================================
  // 3. createTopping Tests
  // ==========================================
  @Nested
  @DisplayName("createTopping tests")
  class CreateToppingTests {

    @Test
    @DisplayName("Tạo thành công: lưu topping status = true và khởi tạo tồn kho = 0")
    void createTopping_Success_SavesToppingAndInitInventory() {
      stubSaveNewTopping();

      ToppingResponse response = service.createTopping(request("Pudding", "6000"));

      ArgumentCaptor<Topping> toppingCaptor = ArgumentCaptor.forClass(Topping.class);
      verify(toppingRepository, times(1)).save(toppingCaptor.capture());
      assertThat(toppingCaptor.getValue().getName()).isEqualTo("Pudding");
      assertThat(toppingCaptor.getValue().getUnitPrice()).isEqualByComparingTo("6000");
      assertThat(toppingCaptor.getValue().getStatus()).isTrue();

      ArgumentCaptor<InventoryTopping> invCaptor = ArgumentCaptor.forClass(InventoryTopping.class);
      verify(inventoryToppingRepository, times(1)).save(invCaptor.capture());
      assertThat(invCaptor.getValue().getStock()).isZero();
      assertThat(invCaptor.getValue().getMinThreshold()).isEqualTo(10);
      assertThat(invCaptor.getValue().getTopping().getId()).isEqualTo(7L);

      assertThat(response.getId()).isEqualTo(7L);
      assertThat(response.getName()).isEqualTo("Pudding");
      assertThat(response.getStock()).isZero();
      assertThat(response.getStatus()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Trân châu", "Trà sữa 2", "Matcha Latte", "Cà phê sữa 3", "Thạch dừa", "ÉCLAIR" })
    @DisplayName("Tên hợp lệ (chữ, số, khoảng trắng, có dấu tiếng Việt) được chấp nhận")
    void createTopping_Success_WhenNameValid(String name) {
      stubSaveNewTopping();

      ToppingResponse response = service.createTopping(request(name, "6000"));

      assertThat(response.getName()).isEqualTo(name);
      verify(toppingRepository).save(any(Topping.class));
      verify(inventoryToppingRepository).save(any(InventoryTopping.class));
    }

    @Test
    @DisplayName("Tên dài đúng 100 ký tự vẫn hợp lệ")
    void createTopping_Success_WhenNameIsMaxLength() {
      stubSaveNewTopping();
      String name = "a".repeat(100);

      ToppingResponse response = service.createTopping(request(name, "6000"));

      assertThat(response.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("Giá nhỏ nhất > 0 vẫn hợp lệ")
    void createTopping_Success_WhenPriceIsSmallestPositive() {
      stubSaveNewTopping();

      ToppingResponse response = service.createTopping(request("Pudding", "1"));

      assertThat(response.getUnitPrice()).isEqualByComparingTo("1");
    }
  }

  // ==========================================
  // 4. updateTopping Tests
  // ==========================================
  @Nested
  @DisplayName("updateTopping tests")
  class UpdateToppingTests {

    @Test
    @DisplayName("Cập nhật tên và giá thành công, giữ nguyên status")
    void updateTopping_Success_UpdatesNameAndPrice() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(tranChau));
      when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(tranChau, 9)));

      ToppingResponse response = service.updateTopping(1L, request("Trân châu trắng", "7000"));

      assertThat(tranChau.getName()).isEqualTo("Trân châu trắng");
      assertThat(tranChau.getUnitPrice()).isEqualByComparingTo("7000");
      assertThat(tranChau.getStatus()).isTrue(); // status không bị đổi
      verify(toppingRepository).save(tranChau);
      assertThat(response.getName()).isEqualTo("Trân châu trắng");
      assertThat(response.getStock()).isEqualTo(9);
    }

    @Test
    @DisplayName("Cập nhật không làm thay đổi tồn kho")
    void updateTopping_DoesNotModifyInventory() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(tranChau));
      when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(tranChau, 9)));

      service.updateTopping(1L, request("Trân châu trắng", "7000"));

      verify(inventoryToppingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giữ nguyên status của topping đang ngừng bán khi cập nhật")
    void updateTopping_KeepsStatus_WhenToppingDiscontinued() {
      when(toppingRepository.findById(2L)).thenReturn(Optional.of(thachDua));
      when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(inventoryToppingRepository.findByToppingId(2L)).thenReturn(Optional.empty());

      ToppingResponse response = service.updateTopping(2L, request("Thạch dừa mới", "4500"));

      assertThat(thachDua.getStatus()).isFalse();
      assertThat(response.getStatus()).isFalse();
      assertThat(response.getStock()).isZero();
    }

    @Test
    @DisplayName("Ném exception và không lưu khi không tìm thấy topping")
    void updateTopping_ThrowsException_WhenNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.updateTopping(99L, request("X", "1")))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("99");

      verify(toppingRepository, never()).save(any());
    }
  }

  // ==========================================
  // 5. updateStatus Tests
  // ==========================================
  @Nested
  @DisplayName("updateStatus tests")
  class UpdateStatusTests {

    @Test
    @DisplayName("Tắt topping: status chuyển thành false")
    void updateStatus_Success_TurnsOff() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(tranChau));
      when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      ToppingResponse response = service.updateStatus(1L, false);

      assertThat(tranChau.getStatus()).isFalse();
      verify(toppingRepository).save(tranChau);
      assertThat(response.getStatus()).isFalse();
      assertThat(response.getStock()).isZero();
    }

    @Test
    @DisplayName("Bật topping: status chuyển thành true")
    void updateStatus_Success_TurnsOn() {
      when(toppingRepository.findById(2L)).thenReturn(Optional.of(thachDua));
      when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(inventoryToppingRepository.findByToppingId(2L)).thenReturn(Optional.empty());

      ToppingResponse response = service.updateStatus(2L, true);

      assertThat(thachDua.getStatus()).isTrue();
      assertThat(response.getStatus()).isTrue();
    }

    @Test
    @DisplayName("Đặt lại đúng status hiện tại vẫn thành công (idempotent)")
    void updateStatus_Success_WhenStatusUnchanged() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(tranChau));
      when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      ToppingResponse response = service.updateStatus(1L, true);

      assertThat(response.getStatus()).isTrue();
    }

    @Test
    @DisplayName("Đổi status không làm đổi tên, giá và tồn kho")
    void updateStatus_DoesNotChangeOtherFields() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(tranChau));
      when(toppingRepository.save(any(Topping.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(tranChau, 12)));

      ToppingResponse response = service.updateStatus(1L, false);

      assertThat(response.getName()).isEqualTo("Trân châu");
      assertThat(response.getUnitPrice()).isEqualByComparingTo("5000");
      assertThat(response.getStock()).isEqualTo(12);
      verify(inventoryToppingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném exception và không lưu khi không tìm thấy topping")
    void updateStatus_ThrowsException_WhenNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.updateStatus(99L, true))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("99");

      verify(toppingRepository, never()).save(any());
    }
  }
}