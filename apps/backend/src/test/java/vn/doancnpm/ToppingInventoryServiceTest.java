package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.service.implement.IToppingInventoryService;
import vn.doancnpm.domain.InventoryLog.ChangeType;
import vn.doancnpm.domain.InventoryTopping;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.dto.Response.InventoryStockResponse;
import vn.doancnpm.repository.InventoryToppingRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.service.InventoryLogService;

@ExtendWith(MockitoExtension.class)
class ToppingInventoryServiceTest {

  @Mock
  private ToppingRepository toppingRepository;

  @Mock
  private InventoryToppingRepository inventoryToppingRepository;

  @Mock
  private InventoryLogService inventoryLogService;

  @InjectMocks
  private IToppingInventoryService service;

  private Topping topping;
  private Order order;

  @BeforeEach
  void setUp() {
    topping = Topping.builder()
        .id(1L)
        .name("Trân châu")
        .unitPrice(new BigDecimal("5000"))
        .status(true)
        .build();

    order = new Order();
    order.setId(100L);
  }

  /** Tạo dòng tồn kho: stock = số lượng đang bán, minThreshold mặc định 10. */
  private InventoryTopping inventory(int stock) {
    return InventoryTopping.builder().id(1L).topping(topping).stock(stock).minThreshold(10).build();
  }

  /** Tạo dòng tồn kho có cả tồn kho tổng (warehouseStock). */
  private InventoryTopping inventory(int stock, int warehouseStock) {
    InventoryTopping inv = inventory(stock);
    inv.setWarehouseStock(warehouseStock);
    return inv;
  }

  // ==========================================
  // 1. deductStock Tests
  // ==========================================
  @Nested
  @DisplayName("deductStock tests")
  class DeductStockTests {

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -100, Integer.MIN_VALUE })
    @DisplayName("Ném exception và không chạm DB khi số lượng trừ <= 0")
    void deductStock_ThrowsException_WhenAmountInvalid(int amount) {
      assertThatThrownBy(() -> service.deductStock(1L, order, amount))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số lượng trừ phải lớn hơn 0");

      verifyNoInteractions(toppingRepository, inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi không tìm thấy topping")
    void deductStock_ThrowsException_WhenToppingNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deductStock(99L, order, 1))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("99");

      verifyNoInteractions(inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception khi topping đã ngừng bán")
    void deductStock_ThrowsException_WhenToppingDiscontinued() {
      topping.setStatus(false);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));

      assertThatThrownBy(() -> service.deductStock(1L, order, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ngừng bán");

      verifyNoInteractions(inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Topping có status = null được xem như ngừng bán")
    void deductStock_ThrowsException_WhenStatusIsNull() {
      topping.setStatus(null);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));

      assertThatThrownBy(() -> service.deductStock(1L, order, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ngừng bán");

      verifyNoInteractions(inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi topping chưa có dòng tồn kho")
    void deductStock_ThrowsException_WhenInventoryRowMissing() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deductStock(1L, order, 1))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Chưa thiết lập kho");

      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception, không lưu, không ghi log khi tồn kho không đủ")
    void deductStock_ThrowsException_WhenStockNotEnough() {
      InventoryTopping inv = inventory(2);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.deductStock(1L, order, 3))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("không đủ số lượng đang bán");

      assertThat(inv.getStock()).isEqualTo(2);
      verify(inventoryToppingRepository, never()).save(any());
      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Biên: trừ nhiều hơn tồn kho đúng 1 đơn vị thì bị từ chối")
    void deductStock_ThrowsException_WhenAmountIsStockPlusOne() {
      InventoryTopping inv = inventory(5);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.deductStock(1L, order, 6))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("không đủ số lượng đang bán");

      assertThat(inv.getStock()).isEqualTo(5);
      verify(inventoryToppingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tồn kho đang bằng 0 thì không thể trừ")
    void deductStock_ThrowsException_WhenStockIsZero() {
      InventoryTopping inv = inventory(0);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.deductStock(1L, order, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("không đủ số lượng đang bán");

      verify(inventoryToppingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trừ kho thành công: giảm stock, lưu và ghi log số âm")
    void deductStock_Success_DeductsSavesAndLogsNegative() {
      InventoryTopping inv = inventory(10);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.deductStock(1L, order, 3);

      assertThat(inv.getStock()).isEqualTo(7);
      verify(inventoryToppingRepository).save(inv);
      verify(inventoryLogService).logToppingChange(topping, order, -3, ChangeType.ORDER_DEDUCT);
    }

    @Test
    @DisplayName("Biên: trừ tối thiểu 1 đơn vị vẫn thành công")
    void deductStock_Success_WhenAmountIsOne() {
      InventoryTopping inv = inventory(10);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.deductStock(1L, order, 1);

      assertThat(inv.getStock()).isEqualTo(9);
      verify(inventoryLogService).logToppingChange(topping, order, -1, ChangeType.ORDER_DEDUCT);
    }

    @Test
    @DisplayName("Biên: trừ đúng bằng tồn kho thì stock về 0")
    void deductStock_Success_WhenAmountEqualsStock() {
      InventoryTopping inv = inventory(5);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.deductStock(1L, order, 5);

      assertThat(inv.getStock()).isZero();
      verify(inventoryToppingRepository).save(inv);
    }

    @Test
    @DisplayName("Trừ kho chỉ ảnh hưởng số lượng đang bán, không đụng tới tồn kho tổng")
    void deductStock_DoesNotChangeWarehouseStock() {
      InventoryTopping inv = inventory(10, 50);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.deductStock(1L, order, 3);

      assertThat(inv.getStock()).isEqualTo(7);
      assertThat(inv.getWarehouseStock()).isEqualTo(50);
    }
  }

  // ==========================================
  // 2. addStock Tests
  // ==========================================
  @Nested
  @DisplayName("addStock tests")
  class AddStockTests {

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, Integer.MIN_VALUE })
    @DisplayName("Ném exception và không chạm DB khi số lượng thêm <= 0")
    void addStock_ThrowsException_WhenAmountInvalid(int amount) {
      assertThatThrownBy(() -> service.addStock(1L, null, amount, "IMPORT"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số lượng thêm phải lớn hơn 0");

      verifyNoInteractions(toppingRepository, inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi không tìm thấy topping")
    void addStock_ThrowsException_WhenToppingNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.addStock(99L, null, 5, "IMPORT"))
          .isInstanceOf(EntityNotFoundException.class);

      verifyNoInteractions(inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Đã có dòng tồn kho: cộng thêm, lưu và ghi log")
    void addStock_Success_WhenInventoryRowExists() {
      InventoryTopping inv = inventory(10);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.addStock(1L, null, 5, "IMPORT");

      assertThat(inv.getStock()).isEqualTo(15);
      verify(inventoryToppingRepository).save(inv);
      verify(inventoryLogService).logToppingChange(eq(topping), isNull(), eq(5), eq(ChangeType.IMPORT), eq("IMPORT"));
    }

    @Test
    @DisplayName("Có order đi kèm: cộng thêm, log ghi order = null và giữ nguyên reason")
    void addStock_Success_LogsWithOrder() {
      InventoryTopping inv = inventory(10);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.addStock(1L, order, 5, "ORDER_CANCEL");

      assertThat(inv.getStock()).isEqualTo(15);
      verify(inventoryToppingRepository).save(inv);
      verify(inventoryLogService).logToppingChange(eq(topping), isNull(), eq(5), eq(ChangeType.IMPORT), eq("ORDER_CANCEL"));
    }

    @Test
    @DisplayName("Tồn kho đang bằng 0 vẫn cộng thêm được")
    void addStock_Success_WhenCurrentStockIsZero() {
      InventoryTopping inv = inventory(0);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.addStock(1L, null, 1, "IMPORT");

      assertThat(inv.getStock()).isEqualTo(1);
      verify(inventoryLogService).logToppingChange(eq(topping), isNull(), eq(1), eq(ChangeType.IMPORT), eq("IMPORT"));
    }

    @Test
    @DisplayName("Thêm vào số lượng đang bán không làm đổi tồn kho tổng")
    void addStock_DoesNotChangeWarehouseStock() {
      InventoryTopping inv = inventory(10, 50);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.addStock(1L, null, 5, "IMPORT");

      assertThat(inv.getStock()).isEqualTo(15);
      assertThat(inv.getWarehouseStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("Chưa có dòng tồn kho: tạo mới với ngưỡng mặc định 10")
    void addStock_CreatesNewRow_WhenInventoryRowMissing() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      service.addStock(1L, null, 20, "IMPORT");

      ArgumentCaptor<InventoryTopping> captor = ArgumentCaptor.forClass(InventoryTopping.class);
      verify(inventoryToppingRepository, times(1)).save(captor.capture());
      InventoryTopping saved = captor.getValue();
      assertThat(saved.getStock()).isEqualTo(20);
      assertThat(saved.getMinThreshold()).isEqualTo(10);
      assertThat(saved.getTopping()).isSameAs(topping);
      verify(inventoryLogService).logToppingChange(eq(topping), isNull(), eq(20), eq(ChangeType.IMPORT), eq("IMPORT"));
    }
  }

  // ==========================================
  // 3. checkStockAvailable Tests
  // ==========================================
  @Nested
  @DisplayName("checkStockAvailable tests")
  class CheckStockAvailableTests {

    @Test
    @DisplayName("Trả về false khi topping không tồn tại")
    void checkStockAvailable_ReturnsFalse_WhenToppingNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      assertThat(service.checkStockAvailable(99L, 1)).isFalse();
      verifyNoInteractions(inventoryToppingRepository);
    }

    @Test
    @DisplayName("Trả về false khi topping đã ngừng bán")
    void checkStockAvailable_ReturnsFalse_WhenToppingDiscontinued() {
      topping.setStatus(false);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));

      assertThat(service.checkStockAvailable(1L, 1)).isFalse();
      verifyNoInteractions(inventoryToppingRepository);
    }

    @Test
    @DisplayName("Trả về false khi chưa có dòng tồn kho")
    void checkStockAvailable_ReturnsFalse_WhenInventoryRowMissing() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      assertThat(service.checkStockAvailable(1L, 1)).isFalse();
    }

    @Test
    @DisplayName("Biên: tồn kho bằng đúng số cần thì trả về true")
    void checkStockAvailable_ReturnsTrue_WhenStockEqualsRequired() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(10)));

      assertThat(service.checkStockAvailable(1L, 10)).isTrue();
    }

    @Test
    @DisplayName("Tồn kho nhiều hơn số cần thì trả về true")
    void checkStockAvailable_ReturnsTrue_WhenStockGreaterThanRequired() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(20)));

      assertThat(service.checkStockAvailable(1L, 10)).isTrue();
    }

    @Test
    @DisplayName("Biên: tồn kho thiếu đúng 1 đơn vị thì trả về false")
    void checkStockAvailable_ReturnsFalse_WhenStockIsOneShort() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(10)));

      assertThat(service.checkStockAvailable(1L, 11)).isFalse();
    }

    @Test
    @DisplayName("Tồn kho ít hơn số cần thì trả về false")
    void checkStockAvailable_ReturnsFalse_WhenStockLessThanRequired() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(5)));

      assertThat(service.checkStockAvailable(1L, 10)).isFalse();
    }

    @Test
    @DisplayName("Hết hàng (stock = 0) thì trả về false")
    void checkStockAvailable_ReturnsFalse_WhenOutOfStock() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(0)));

      assertThat(service.checkStockAvailable(1L, 1)).isFalse();
    }
  }

  // ==========================================
  // 4. getStock Tests
  // ==========================================
  @Nested
  @DisplayName("getStock tests")
  class GetStockTests {

    @Test
    @DisplayName("Trả về response đầy đủ khi có dòng tồn kho")
    void getStock_Success_ReturnsFullResponse() {
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(25)));

      InventoryStockResponse response = service.getStock(1L);

      assertThat(response.getItemId()).isEqualTo(1L);
      assertThat(response.getItemName()).isEqualTo("Trân châu");
      assertThat(response.getStock()).isEqualTo(25);
      assertThat(response.getMinThreshold()).isEqualTo(10);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi chưa có dòng tồn kho")
    void getStock_ThrowsException_WhenInventoryRowMissing() {
      when(inventoryToppingRepository.findByToppingId(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getStock(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("99");
    }
  }

  // ==========================================
  // 5. addWarehouseStock Tests
  // ==========================================
  @Nested
  @DisplayName("addWarehouseStock tests")
  class AddWarehouseStockTests {

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -5, Integer.MIN_VALUE })
    @DisplayName("Ném exception khi số lượng nhập tồn kho <= 0")
    void addWarehouseStock_ThrowsException_WhenAmountInvalid(int amount) {
      assertThatThrownBy(() -> service.addWarehouseStock(1L, amount, "NHẬP KHO"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số lượng nhập tồn kho phải lớn hơn 0");

      verifyNoInteractions(inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi không tìm thấy topping")
    void addWarehouseStock_ThrowsException_WhenToppingNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.addWarehouseStock(99L, 10, "NHẬP KHO"))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("99");

      verifyNoInteractions(inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Đã có dòng tồn kho: cộng vào warehouseStock và ghi log")
    void addWarehouseStock_Success_WhenInventoryRowExists() {
      InventoryTopping inv = inventory(10, 50);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.addWarehouseStock(1L, 20, "NHẬP TỒN KHO");

      assertThat(inv.getWarehouseStock()).isEqualTo(70);
      verify(inventoryToppingRepository).save(inv);
      verify(inventoryLogService).logToppingChange(eq(topping), isNull(), eq(20), eq(ChangeType.IMPORT), eq("NHẬP TỒN KHO"));
    }

    @Test
    @DisplayName("Nhập kho không làm đổi số lượng đang bán")
    void addWarehouseStock_DoesNotChangeSellingStock() {
      InventoryTopping inv = inventory(10, 50);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.addWarehouseStock(1L, 1, "NHẬP KHO");

      assertThat(inv.getStock()).isEqualTo(10);
      assertThat(inv.getWarehouseStock()).isEqualTo(51);
    }

    @Test
    @DisplayName("Chưa có dòng tồn kho: tạo mới; reason = null thì log dùng lý do mặc định NHẬP KHO")
    void addWarehouseStock_CreatesNewRow_WhenInventoryRowMissing() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      service.addWarehouseStock(1L, 30, null);

      ArgumentCaptor<InventoryTopping> captor = ArgumentCaptor.forClass(InventoryTopping.class);
      verify(inventoryToppingRepository).save(captor.capture());
      InventoryTopping saved = captor.getValue();
      assertThat(saved.getWarehouseStock()).isEqualTo(30);
      assertThat(saved.getStock()).isZero();
      assertThat(saved.getMinThreshold()).isEqualTo(10);
      assertThat(saved.getTopping()).isSameAs(topping);
      verify(inventoryLogService).logToppingChange(eq(topping), isNull(), eq(30), eq(ChangeType.IMPORT), eq("NHẬP KHO"));
    }
  }

  // ==========================================
  // 6. transferToSelling Tests
  // ==========================================
  @Nested
  @DisplayName("transferToSelling tests")
  class TransferToSellingTests {

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, Integer.MIN_VALUE })
    @DisplayName("Ném exception khi số lượng chuyển <= 0")
    void transferToSelling_ThrowsException_WhenAmountInvalid(int amount) {
      assertThatThrownBy(() -> service.transferToSelling(1L, amount))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số lượng chuyển sang bán phải lớn hơn 0");

      verifyNoInteractions(toppingRepository, inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy topping")
    void transferToSelling_ThrowsException_WhenToppingNotFound() {
      when(toppingRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.transferToSelling(99L, 5))
          .isInstanceOf(EntityNotFoundException.class);

      verifyNoInteractions(inventoryToppingRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi chưa có bản ghi tồn kho")
    void transferToSelling_ThrowsException_WhenInventoryRowMissing() {
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.transferToSelling(1L, 5))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Topping chưa có bản ghi tồn kho");

      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception khi tồn kho tổng không đủ để chuyển")
    void transferToSelling_ThrowsException_WhenWarehouseStockNotEnough() {
      InventoryTopping inv = inventory(10, 5);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.transferToSelling(1L, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Số lượng tồn kho không đủ để chuyển sang bán");

      assertThat(inv.getWarehouseStock()).isEqualTo(5);
      assertThat(inv.getStock()).isEqualTo(10);
      verify(inventoryToppingRepository, never()).save(any());
      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Biên: chuyển nhiều hơn tồn kho tổng đúng 1 đơn vị thì bị từ chối")
    void transferToSelling_ThrowsException_WhenAmountIsWarehouseStockPlusOne() {
      InventoryTopping inv = inventory(10, 50);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.transferToSelling(1L, 51))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Số lượng tồn kho không đủ để chuyển sang bán");

      verify(inventoryToppingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chuyển thành công: trừ tồn kho tổng, cộng vào quầy bán, ghi log")
    void transferToSelling_Success_MovesStockFromWarehouseToSelling() {
      InventoryTopping inv = inventory(10, 50);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.transferToSelling(1L, 15);

      assertThat(inv.getWarehouseStock()).isEqualTo(35); // 50 - 15
      assertThat(inv.getStock()).isEqualTo(25); // 10 + 15
      verify(inventoryToppingRepository).save(inv);
      verify(inventoryLogService).logToppingChange(eq(topping), isNull(), eq(15), eq(ChangeType.TRANSFER), anyString());
    }

    @Test
    @DisplayName("Biên: chuyển đúng bằng tồn kho tổng thì kho tổng về 0")
    void transferToSelling_Success_WhenAmountEqualsWarehouseStock() {
      InventoryTopping inv = inventory(0, 20);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.transferToSelling(1L, 20);

      assertThat(inv.getWarehouseStock()).isZero();
      assertThat(inv.getStock()).isEqualTo(20);
      verify(inventoryToppingRepository).save(inv);
    }

    @Test
    @DisplayName("Biên: chuyển tối thiểu 1 đơn vị vẫn thành công")
    void transferToSelling_Success_WhenAmountIsOne() {
      InventoryTopping inv = inventory(10, 50);
      when(toppingRepository.findById(1L)).thenReturn(Optional.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inv));

      service.transferToSelling(1L, 1);

      assertThat(inv.getWarehouseStock()).isEqualTo(49);
      assertThat(inv.getStock()).isEqualTo(11);
    }
  }

  // ==========================================
  // 7. getAllToppingStocks Tests
  // ==========================================
  @Nested
  @DisplayName("getAllToppingStocks tests")
  class GetAllToppingStocksTests {

    @Test
    @DisplayName("Trả về danh sách rỗng khi chưa có topping nào")
    void getAllToppingStocks_ReturnsEmptyList_WhenNoToppings() {
      when(toppingRepository.findAll()).thenReturn(Collections.emptyList());

      assertThat(service.getAllToppingStocks()).isEmpty();
      verifyNoInteractions(inventoryToppingRepository);
    }

    @Test
    @DisplayName("Map đầy đủ field cho một topping")
    void getAllToppingStocks_MapsAllFields() {
      when(toppingRepository.findAll()).thenReturn(List.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(20, 100)));

      List<InventoryStockResponse> list = service.getAllToppingStocks();

      assertThat(list).hasSize(1);
      InventoryStockResponse resp = list.get(0);
      assertThat(resp.getItemId()).isEqualTo(1L);
      assertThat(resp.getItemName()).isEqualTo("Trân châu");
      assertThat(resp.getItemType()).isEqualTo("TOPPING");
      assertThat(resp.getStock()).isEqualTo(20);
      assertThat(resp.getWarehouseStock()).isEqualTo(100);
      assertThat(resp.getMinThreshold()).isEqualTo(10);
    }

    @Test
    @DisplayName("Chưa có bản ghi tồn kho: tạo mới mặc định và lưu")
    void getAllToppingStocks_CreatesDefaultRow_WhenInventoryRowMissing() {
      when(toppingRepository.findAll()).thenReturn(List.of(topping));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.empty());
      when(inventoryToppingRepository.save(any(InventoryTopping.class))).thenAnswer(i -> {
        InventoryTopping item = i.getArgument(0);
        item.setId(88L);
        return item;
      });

      List<InventoryStockResponse> list = service.getAllToppingStocks();

      assertThat(list).hasSize(1);
      assertThat(list.get(0).getStock()).isZero();
      assertThat(list.get(0).getWarehouseStock()).isEqualTo(200);
      verify(inventoryToppingRepository).save(any(InventoryTopping.class));
    }

    @Test
    @DisplayName("Nhiều topping: giữ nguyên thứ tự và map đúng tồn kho từng topping")
    void getAllToppingStocks_KeepsOrder_ForMultipleToppings() {
      Topping jelly = Topping.builder()
          .id(2L).name("Thạch dừa").unitPrice(new BigDecimal("4000")).status(true).build();
      InventoryTopping jellyInv = InventoryTopping.builder()
          .id(2L).topping(jelly).stock(7).minThreshold(10).build();
      jellyInv.setWarehouseStock(70);

      when(toppingRepository.findAll()).thenReturn(List.of(topping, jelly));
      when(inventoryToppingRepository.findByToppingId(1L)).thenReturn(Optional.of(inventory(20, 100)));
      when(inventoryToppingRepository.findByToppingId(2L)).thenReturn(Optional.of(jellyInv));

      List<InventoryStockResponse> list = service.getAllToppingStocks();

      assertThat(list).hasSize(2);
      assertThat(list).extracting(InventoryStockResponse::getItemId).containsExactly(1L, 2L);
      assertThat(list.get(1).getItemName()).isEqualTo("Thạch dừa");
      assertThat(list.get(1).getStock()).isEqualTo(7);
      assertThat(list.get(1).getWarehouseStock()).isEqualTo(70);
    }
  }
}