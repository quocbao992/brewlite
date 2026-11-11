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
import vn.doancnpm.service.implement.IProductInventoryService;
import vn.doancnpm.domain.InventoryLog.ChangeType;
import vn.doancnpm.domain.InventoryProduct;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Product;
import vn.doancnpm.dto.Response.InventoryStockResponse;
import vn.doancnpm.repository.InventoryProductRepository;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.service.InventoryLogService;

@ExtendWith(MockitoExtension.class)
class ProductInventoryServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private InventoryProductRepository inventoryProductRepository;

  @Mock
  private InventoryLogService inventoryLogService;

  @InjectMocks
  private IProductInventoryService service;

  private Product product;
  private Order order;

  @BeforeEach
  void setUp() {
    product = Product.builder()
        .id(1L)
        .name("Cà phê sữa")
        .unitPrice(new BigDecimal("25000"))
        .status(true)
        .build();

    order = new Order();
    order.setId(100L);
  }

  /** Tạo dòng tồn kho: stock = số lượng đang bán, minThreshold mặc định 5. */
  private InventoryProduct inventory(int stock) {
    return InventoryProduct.builder().id(1L).product(product).stock(stock).minThreshold(5).build();
  }

  /** Tạo dòng tồn kho có cả tồn kho tổng (warehouseStock). */
  private InventoryProduct inventory(int stock, int warehouseStock) {
    InventoryProduct inv = inventory(stock);
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

      verifyNoInteractions(productRepository, inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi không tìm thấy sản phẩm")
    void deductStock_ThrowsException_WhenProductNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deductStock(99L, order, 1))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("99");

      verifyNoInteractions(inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception khi sản phẩm đã ngừng bán")
    void deductStock_ThrowsException_WhenProductDiscontinued() {
      product.setStatus(false);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> service.deductStock(1L, order, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ngừng bán");

      verifyNoInteractions(inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi sản phẩm chưa có dòng tồn kho")
    void deductStock_ThrowsException_WhenInventoryRowMissing() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deductStock(1L, order, 1))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Chưa thiết lập kho");

      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception, không lưu, không ghi log khi tồn kho không đủ")
    void deductStock_ThrowsException_WhenStockNotEnough() {
      InventoryProduct inv = inventory(2);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.deductStock(1L, order, 3))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("không đủ số lượng đang bán");

      assertThat(inv.getStock()).isEqualTo(2);
      verify(inventoryProductRepository, never()).save(any());
      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Biên: trừ nhiều hơn tồn kho đúng 1 đơn vị thì bị từ chối")
    void deductStock_ThrowsException_WhenAmountIsStockPlusOne() {
      InventoryProduct inv = inventory(5);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.deductStock(1L, order, 6))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("không đủ số lượng đang bán");

      assertThat(inv.getStock()).isEqualTo(5);
      verify(inventoryProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tồn kho đang bằng 0 thì không thể trừ")
    void deductStock_ThrowsException_WhenStockIsZero() {
      InventoryProduct inv = inventory(0);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.deductStock(1L, order, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("không đủ số lượng đang bán");

      verify(inventoryProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trừ kho thành công: giảm stock, lưu và ghi log số âm")
    void deductStock_Success_DeductsSavesAndLogsNegative() {
      InventoryProduct inv = inventory(10);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.deductStock(1L, order, 4);

      assertThat(inv.getStock()).isEqualTo(6);
      verify(inventoryProductRepository).save(inv);
      verify(inventoryLogService).logProductChange(product, order, -4, ChangeType.ORDER_DEDUCT);
    }

    @Test
    @DisplayName("Biên: trừ tối thiểu 1 đơn vị vẫn thành công")
    void deductStock_Success_WhenAmountIsOne() {
      InventoryProduct inv = inventory(10);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.deductStock(1L, order, 1);

      assertThat(inv.getStock()).isEqualTo(9);
      verify(inventoryLogService).logProductChange(product, order, -1, ChangeType.ORDER_DEDUCT);
    }

    @Test
    @DisplayName("Biên: trừ đúng bằng tồn kho thì stock về 0")
    void deductStock_Success_WhenAmountEqualsStock() {
      InventoryProduct inv = inventory(5);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.deductStock(1L, order, 5);

      assertThat(inv.getStock()).isZero();
      verify(inventoryProductRepository).save(inv);
    }

    @Test
    @DisplayName("Trừ kho chỉ ảnh hưởng số lượng đang bán, không đụng tới tồn kho tổng")
    void deductStock_DoesNotChangeWarehouseStock() {
      InventoryProduct inv = inventory(10, 50);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

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

      verifyNoInteractions(productRepository, inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi không tìm thấy sản phẩm")
    void addStock_ThrowsException_WhenProductNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.addStock(99L, null, 5, "IMPORT"))
          .isInstanceOf(EntityNotFoundException.class);

      verifyNoInteractions(inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Đã có dòng tồn kho: cộng thêm, lưu và ghi log")
    void addStock_Success_WhenInventoryRowExists() {
      InventoryProduct inv = inventory(10);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.addStock(1L, order, 5, "ORDER_CANCEL");

      assertThat(inv.getStock()).isEqualTo(15);
      verify(inventoryProductRepository).save(inv);
      verify(inventoryLogService).logProductChange(eq(product), isNull(), eq(5), eq(ChangeType.IMPORT), eq("ORDER_CANCEL"));
    }

    @Test
    @DisplayName("Tồn kho đang bằng 0 vẫn cộng thêm được")
    void addStock_Success_WhenCurrentStockIsZero() {
      InventoryProduct inv = inventory(0);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.addStock(1L, null, 1, "IMPORT");

      assertThat(inv.getStock()).isEqualTo(1);
      verify(inventoryLogService).logProductChange(eq(product), isNull(), eq(1), eq(ChangeType.IMPORT), eq("IMPORT"));
    }

    @Test
    @DisplayName("Thêm vào số lượng đang bán không làm đổi tồn kho tổng")
    void addStock_DoesNotChangeWarehouseStock() {
      InventoryProduct inv = inventory(10, 50);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.addStock(1L, null, 5, "IMPORT");

      assertThat(inv.getStock()).isEqualTo(15);
      assertThat(inv.getWarehouseStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("Chưa có dòng tồn kho: tạo mới với ngưỡng mặc định 5")
    void addStock_CreatesNewRow_WhenInventoryRowMissing() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.empty());

      service.addStock(1L, null, 20, "IMPORT");

      ArgumentCaptor<InventoryProduct> captor = ArgumentCaptor.forClass(InventoryProduct.class);
      verify(inventoryProductRepository, times(1)).save(captor.capture());
      InventoryProduct saved = captor.getValue();
      assertThat(saved.getStock()).isEqualTo(20);
      assertThat(saved.getMinThreshold()).isEqualTo(5);
      assertThat(saved.getProduct()).isSameAs(product);
      verify(inventoryLogService).logProductChange(eq(product), isNull(), eq(20), eq(ChangeType.IMPORT), eq("IMPORT"));
    }
  }

  // ==========================================
  // 3. checkStockAvailable Tests
  // ==========================================
  @Nested
  @DisplayName("checkStockAvailable tests")
  class CheckStockAvailableTests {

    @Test
    @DisplayName("Trả về false khi sản phẩm không tồn tại")
    void checkStockAvailable_ReturnsFalse_WhenProductNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThat(service.checkStockAvailable(99L, 1)).isFalse();
      verifyNoInteractions(inventoryProductRepository);
    }

    @Test
    @DisplayName("Trả về false khi sản phẩm đã ngừng bán")
    void checkStockAvailable_ReturnsFalse_WhenProductDiscontinued() {
      product.setStatus(false);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));

      assertThat(service.checkStockAvailable(1L, 1)).isFalse();
      verifyNoInteractions(inventoryProductRepository);
    }

    @Test
    @DisplayName("Trả về false khi chưa có dòng tồn kho")
    void checkStockAvailable_ReturnsFalse_WhenInventoryRowMissing() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.empty());

      assertThat(service.checkStockAvailable(1L, 1)).isFalse();
    }

    @Test
    @DisplayName("Biên: tồn kho bằng đúng số cần thì trả về true")
    void checkStockAvailable_ReturnsTrue_WhenStockEqualsRequired() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(10)));

      assertThat(service.checkStockAvailable(1L, 10)).isTrue();
    }

    @Test
    @DisplayName("Tồn kho nhiều hơn số cần thì trả về true")
    void checkStockAvailable_ReturnsTrue_WhenStockGreaterThanRequired() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(20)));

      assertThat(service.checkStockAvailable(1L, 10)).isTrue();
    }

    @Test
    @DisplayName("Biên: tồn kho thiếu đúng 1 đơn vị thì trả về false")
    void checkStockAvailable_ReturnsFalse_WhenStockIsOneShort() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(10)));

      assertThat(service.checkStockAvailable(1L, 11)).isFalse();
    }

    @Test
    @DisplayName("Tồn kho ít hơn số cần thì trả về false")
    void checkStockAvailable_ReturnsFalse_WhenStockLessThanRequired() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(5)));

      assertThat(service.checkStockAvailable(1L, 10)).isFalse();
    }

    @Test
    @DisplayName("Hết hàng (stock = 0) thì trả về false")
    void checkStockAvailable_ReturnsFalse_WhenOutOfStock() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(0)));

      assertThat(service.checkStockAvailable(1L, 1)).isFalse();
    }
  }

  // ==========================================
  // 4. getStock / getAvailableStock Tests
  // ==========================================
  @Nested
  @DisplayName("getStock / getAvailableStock tests")
  class GetStockTests {

    @Test
    @DisplayName("getStock trả về response đầy đủ khi có dòng tồn kho")
    void getStock_Success_ReturnsFullResponse() {
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(25)));

      InventoryStockResponse response = service.getStock(1L);

      assertThat(response.getItemId()).isEqualTo(1L);
      assertThat(response.getItemName()).isEqualTo("Cà phê sữa");
      assertThat(response.getStock()).isEqualTo(25);
      assertThat(response.getMinThreshold()).isEqualTo(5);
    }

    @Test
    @DisplayName("getStock ném EntityNotFoundException khi chưa có dòng tồn kho")
    void getStock_ThrowsException_WhenInventoryRowMissing() {
      when(inventoryProductRepository.findByProductId(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getStock(99L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getAvailableStock trả về số lượng đang bán")
    void getAvailableStock_ReturnsStock_WhenInventoryRowExists() {
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(12)));

      assertThat(service.getAvailableStock(1L)).isEqualTo(12);
    }

    @Test
    @DisplayName("getAvailableStock trả về 0 khi chưa có dòng tồn kho")
    void getAvailableStock_ReturnsZero_WhenInventoryRowMissing() {
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.empty());

      assertThat(service.getAvailableStock(1L)).isZero();
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

      verifyNoInteractions(productRepository, inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném EntityNotFoundException khi không tìm thấy sản phẩm")
    void addWarehouseStock_ThrowsException_WhenProductNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.addWarehouseStock(99L, 10, "NHẬP KHO"))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("99");

      verifyNoInteractions(inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Đã có dòng tồn kho: cộng vào warehouseStock và ghi log")
    void addWarehouseStock_Success_WhenInventoryRowExists() {
      InventoryProduct inv = inventory(10, 50);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.addWarehouseStock(1L, 20, "NHẬP TỒN KHO");

      assertThat(inv.getWarehouseStock()).isEqualTo(70);
      verify(inventoryProductRepository).save(inv);
      verify(inventoryLogService).logProductChange(eq(product), isNull(), eq(20), eq(ChangeType.IMPORT), eq("NHẬP TỒN KHO"));
    }

    @Test
    @DisplayName("Nhập kho không làm đổi số lượng đang bán")
    void addWarehouseStock_DoesNotChangeSellingStock() {
      InventoryProduct inv = inventory(10, 50);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.addWarehouseStock(1L, 1, "NHẬP KHO");

      assertThat(inv.getStock()).isEqualTo(10);
      assertThat(inv.getWarehouseStock()).isEqualTo(51);
    }

    // Giả định giống IToppingInventoryService: chưa có dòng tồn kho -> tạo mới.
    // Nếu service sản phẩm xử lý khác (ném exception) thì chỉnh lại test này.
    @Test
    @DisplayName("Chưa có dòng tồn kho: tạo mới với stock = 0 và ghi log")
    void addWarehouseStock_CreatesNewRow_WhenInventoryRowMissing() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.empty());

      service.addWarehouseStock(1L, 30, "NHẬP KHO");

      ArgumentCaptor<InventoryProduct> captor = ArgumentCaptor.forClass(InventoryProduct.class);
      verify(inventoryProductRepository).save(captor.capture());
      InventoryProduct saved = captor.getValue();
      assertThat(saved.getWarehouseStock()).isEqualTo(30);
      assertThat(saved.getStock()).isZero();
      assertThat(saved.getMinThreshold()).isEqualTo(5);
      assertThat(saved.getProduct()).isSameAs(product);
      verify(inventoryLogService).logProductChange(eq(product), isNull(), eq(30), eq(ChangeType.IMPORT), eq("NHẬP KHO"));
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

      verifyNoInteractions(productRepository, inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception khi không tìm thấy sản phẩm")
    void transferToSelling_ThrowsException_WhenProductNotFound() {
      when(productRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.transferToSelling(99L, 5))
          .isInstanceOf(RuntimeException.class);

      verifyNoInteractions(inventoryProductRepository, inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception khi chưa có dòng tồn kho")
    void transferToSelling_ThrowsException_WhenInventoryRowMissing() {
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.transferToSelling(1L, 5))
          .isInstanceOf(RuntimeException.class);

      verify(inventoryProductRepository, never()).save(any());
      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Ném exception khi tồn kho tổng không đủ để chuyển")
    void transferToSelling_ThrowsException_WhenWarehouseStockNotEnough() {
      InventoryProduct inv = inventory(10, 5);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.transferToSelling(1L, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Số lượng tồn kho không đủ để chuyển sang bán");

      assertThat(inv.getWarehouseStock()).isEqualTo(5);
      assertThat(inv.getStock()).isEqualTo(10);
      verify(inventoryProductRepository, never()).save(any());
      verifyNoInteractions(inventoryLogService);
    }

    @Test
    @DisplayName("Biên: chuyển nhiều hơn tồn kho tổng đúng 1 đơn vị thì bị từ chối")
    void transferToSelling_ThrowsException_WhenAmountIsWarehouseStockPlusOne() {
      InventoryProduct inv = inventory(10, 50);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      assertThatThrownBy(() -> service.transferToSelling(1L, 51))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Số lượng tồn kho không đủ để chuyển sang bán");

      verify(inventoryProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chuyển thành công: trừ tồn kho tổng, cộng vào quầy bán, ghi log")
    void transferToSelling_Success_MovesStockFromWarehouseToSelling() {
      InventoryProduct inv = inventory(10, 50);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.transferToSelling(1L, 15);

      assertThat(inv.getWarehouseStock()).isEqualTo(35);
      assertThat(inv.getStock()).isEqualTo(25);
      verify(inventoryProductRepository).save(inv);
      verify(inventoryLogService).logProductChange(eq(product), isNull(), eq(15), eq(ChangeType.TRANSFER), anyString());
    }

    @Test
    @DisplayName("Biên: chuyển đúng bằng tồn kho tổng thì kho tổng về 0")
    void transferToSelling_Success_WhenAmountEqualsWarehouseStock() {
      InventoryProduct inv = inventory(0, 20);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.transferToSelling(1L, 20);

      assertThat(inv.getWarehouseStock()).isZero();
      assertThat(inv.getStock()).isEqualTo(20);
      verify(inventoryProductRepository).save(inv);
    }

    @Test
    @DisplayName("Biên: chuyển tối thiểu 1 đơn vị vẫn thành công")
    void transferToSelling_Success_WhenAmountIsOne() {
      InventoryProduct inv = inventory(10, 50);
      when(productRepository.findById(1L)).thenReturn(Optional.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inv));

      service.transferToSelling(1L, 1);

      assertThat(inv.getWarehouseStock()).isEqualTo(49);
      assertThat(inv.getStock()).isEqualTo(11);
    }
  }

  // ==========================================
  // 7. getAllProductStocks Tests
  // ==========================================
  @Nested
  @DisplayName("getAllProductStocks tests")
  class GetAllProductStocksTests {

    @Test
    @DisplayName("Trả về danh sách rỗng khi chưa có sản phẩm nào")
    void getAllProductStocks_ReturnsEmptyList_WhenNoProducts() {
      when(productRepository.findAll()).thenReturn(Collections.emptyList());

      assertThat(service.getAllProductStocks()).isEmpty();
      verifyNoInteractions(inventoryProductRepository);
    }

    @Test
    @DisplayName("Map đầy đủ field cho một sản phẩm")
    void getAllProductStocks_MapsAllFields() {
      when(productRepository.findAll()).thenReturn(List.of(product));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(20, 100)));

      List<InventoryStockResponse> list = service.getAllProductStocks();

      assertThat(list).hasSize(1);
      InventoryStockResponse resp = list.get(0);
      assertThat(resp.getItemId()).isEqualTo(1L);
      assertThat(resp.getItemName()).isEqualTo("Cà phê sữa");
      assertThat(resp.getItemType()).isEqualTo("PRODUCT");
      assertThat(resp.getStock()).isEqualTo(20);
      assertThat(resp.getWarehouseStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("Nhiều sản phẩm: giữ nguyên thứ tự và map đúng tồn kho từng sản phẩm")
    void getAllProductStocks_KeepsOrder_ForMultipleProducts() {
      Product tea = Product.builder()
          .id(2L).name("Trà đào").unitPrice(new BigDecimal("30000")).status(true).build();
      InventoryProduct teaInv = InventoryProduct.builder()
          .id(2L).product(tea).stock(7).minThreshold(5).build();
      teaInv.setWarehouseStock(70);

      when(productRepository.findAll()).thenReturn(List.of(product, tea));
      when(inventoryProductRepository.findByProductId(1L)).thenReturn(Optional.of(inventory(20, 100)));
      when(inventoryProductRepository.findByProductId(2L)).thenReturn(Optional.of(teaInv));

      List<InventoryStockResponse> list = service.getAllProductStocks();

      assertThat(list).hasSize(2);
      assertThat(list).extracting(InventoryStockResponse::getItemId).containsExactly(1L, 2L);
      assertThat(list.get(1).getItemName()).isEqualTo("Trà đào");
      assertThat(list.get(1).getStock()).isEqualTo(7);
      assertThat(list.get(1).getWarehouseStock()).isEqualTo(70);
    }
  }
}