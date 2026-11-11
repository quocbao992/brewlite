package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.doancnpm.service.implement.IInventoryLogService;
import vn.doancnpm.domain.InventoryLog.ChangeType;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.ProductLog;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.domain.ToppingLog;
import vn.doancnpm.dto.Response.InventoryLogResponse;
import vn.doancnpm.repository.ProductLogRepository;
import vn.doancnpm.repository.ToppingLogRepository;

@ExtendWith(MockitoExtension.class)
class InventoryLogServiceTest {

  @Mock
  private ProductLogRepository productLogRepository;

  @Mock
  private ToppingLogRepository toppingLogRepository;

  @InjectMocks
  private IInventoryLogService service;

  private Product product;
  private Topping topping;
  private Order order;

  @BeforeEach
  void setUp() {
    product = Product.builder()
        .id(1L)
        .name("Cà phê sữa")
        .unitPrice(new BigDecimal("25000"))
        .build();

    topping = Topping.builder()
        .id(2L)
        .name("Trân châu")
        .unitPrice(new BigDecimal("5000"))
        .build();

    order = new Order();
    order.setId(100L);
  }

  // ==========================================
  // 1. logProductChange Tests
  // ==========================================
  @Nested
  @DisplayName("logProductChange tests")
  class LogProductChangeTests {

    @Test
    @DisplayName("Lưu ProductLog đúng dữ liệu và gán createdAt")
    void logProductChange_Success_SavesCorrectData() {
      LocalDateTime before = LocalDateTime.now();

      service.logProductChange(product, order, -3, ChangeType.ORDER_DEDUCT);

      LocalDateTime after = LocalDateTime.now();
      ArgumentCaptor<ProductLog> captor = ArgumentCaptor.forClass(ProductLog.class);
      verify(productLogRepository, times(1)).save(captor.capture());
      ProductLog saved = captor.getValue();
      assertThat(saved.getProduct()).isSameAs(product);
      assertThat(saved.getOrder()).isSameAs(order);
      assertThat(saved.getQuantityChange()).isEqualTo(-3);
      assertThat(saved.getChangeType()).isEqualTo(ChangeType.ORDER_DEDUCT);
      assertThat(saved.getCreatedAt()).isBetween(before, after);
      verifyNoInteractions(toppingLogRepository);
    }

    @Test
    @DisplayName("Không có order thì order trong log là null")
    void logProductChange_OrderIsNull_WhenNoOrder() {
      service.logProductChange(product, null, 10, ChangeType.IMPORT);

      ArgumentCaptor<ProductLog> captor = ArgumentCaptor.forClass(ProductLog.class);
      verify(productLogRepository).save(captor.capture());
      assertThat(captor.getValue().getOrder()).isNull();
      assertThat(captor.getValue().getQuantityChange()).isEqualTo(10);
    }

    @ParameterizedTest
    @EnumSource(ChangeType.class)
    @DisplayName("Ghi log được với mọi loại ChangeType")
    void logProductChange_Success_ForEveryChangeType(ChangeType type) {
      service.logProductChange(product, null, 5, type);

      ArgumentCaptor<ProductLog> captor = ArgumentCaptor.forClass(ProductLog.class);
      verify(productLogRepository).save(captor.capture());
      assertThat(captor.getValue().getChangeType()).isEqualTo(type);
    }

    @Test
    @DisplayName("Giữ nguyên dấu của số lượng: số dương (nhập) và số âm (xuất)")
    void logProductChange_KeepsSignOfQuantity() {
      service.logProductChange(product, null, 7, ChangeType.IMPORT);
      service.logProductChange(product, order, -7, ChangeType.ORDER_DEDUCT);

      ArgumentCaptor<ProductLog> captor = ArgumentCaptor.forClass(ProductLog.class);
      verify(productLogRepository, times(2)).save(captor.capture());
      assertThat(captor.getAllValues()).extracting(ProductLog::getQuantityChange).containsExactly(7, -7);
    }
  }

  // ==========================================
  // 2. logToppingChange Tests
  // ==========================================
  @Nested
  @DisplayName("logToppingChange tests")
  class LogToppingChangeTests {

    @Test
    @DisplayName("Lưu ToppingLog đúng dữ liệu và gán createdAt")
    void logToppingChange_Success_SavesCorrectData() {
      LocalDateTime before = LocalDateTime.now();

      service.logToppingChange(topping, order, 20, ChangeType.IMPORT);

      LocalDateTime after = LocalDateTime.now();
      ArgumentCaptor<ToppingLog> captor = ArgumentCaptor.forClass(ToppingLog.class);
      verify(toppingLogRepository, times(1)).save(captor.capture());
      ToppingLog saved = captor.getValue();
      assertThat(saved.getTopping()).isSameAs(topping);
      assertThat(saved.getOrder()).isSameAs(order);
      assertThat(saved.getQuantityChange()).isEqualTo(20);
      assertThat(saved.getChangeType()).isEqualTo(ChangeType.IMPORT);
      assertThat(saved.getCreatedAt()).isBetween(before, after);
      verifyNoInteractions(productLogRepository);
    }

    @Test
    @DisplayName("Không có order thì order trong log là null")
    void logToppingChange_OrderIsNull_WhenNoOrder() {
      service.logToppingChange(topping, null, 15, ChangeType.IMPORT);

      ArgumentCaptor<ToppingLog> captor = ArgumentCaptor.forClass(ToppingLog.class);
      verify(toppingLogRepository).save(captor.capture());
      assertThat(captor.getValue().getOrder()).isNull();
    }

    @ParameterizedTest
    @EnumSource(ChangeType.class)
    @DisplayName("Ghi log được với mọi loại ChangeType")
    void logToppingChange_Success_ForEveryChangeType(ChangeType type) {
      service.logToppingChange(topping, null, 5, type);

      ArgumentCaptor<ToppingLog> captor = ArgumentCaptor.forClass(ToppingLog.class);
      verify(toppingLogRepository).save(captor.capture());
      assertThat(captor.getValue().getChangeType()).isEqualTo(type);
    }

    @Test
    @DisplayName("Giữ nguyên dấu của số lượng: số dương (nhập) và số âm (xuất)")
    void logToppingChange_KeepsSignOfQuantity() {
      service.logToppingChange(topping, null, 9, ChangeType.IMPORT);
      service.logToppingChange(topping, order, -9, ChangeType.ORDER_DEDUCT);

      ArgumentCaptor<ToppingLog> captor = ArgumentCaptor.forClass(ToppingLog.class);
      verify(toppingLogRepository, times(2)).save(captor.capture());
      assertThat(captor.getAllValues()).extracting(ToppingLog::getQuantityChange).containsExactly(9, -9);
    }
  }

  // ==========================================
  // 3. Validation tests (CẦN BỔ SUNG VÀO SERVICE)
  //    Các test này sẽ FAIL cho tới khi IInventoryLogService
  //    kiểm tra tham số đầu vào trước khi gọi repository.
  // ==========================================
  @Nested
  @DisplayName("Validation tests (cần bổ sung vào service)")
  class ValidationTests {

    @Test
    @DisplayName("Ném exception và không lưu khi product null")
    void logProductChange_ThrowsException_WhenProductNull() {
      assertThatThrownBy(() -> service.logProductChange(null, order, 5, ChangeType.IMPORT))
          .isInstanceOf(IllegalArgumentException.class);

      verifyNoInteractions(productLogRepository);
    }

    @Test
    @DisplayName("Ném exception và không lưu khi topping null")
    void logToppingChange_ThrowsException_WhenToppingNull() {
      assertThatThrownBy(() -> service.logToppingChange(null, order, 5, ChangeType.IMPORT))
          .isInstanceOf(IllegalArgumentException.class);

      verifyNoInteractions(toppingLogRepository);
    }

    @Test
    @DisplayName("Ném exception và không lưu khi changeType null (log sản phẩm)")
    void logProductChange_ThrowsException_WhenChangeTypeNull() {
      assertThatThrownBy(() -> service.logProductChange(product, order, 5, null))
          .isInstanceOf(IllegalArgumentException.class);

      verifyNoInteractions(productLogRepository);
    }

    @Test
    @DisplayName("Ném exception và không lưu khi changeType null (log topping)")
    void logToppingChange_ThrowsException_WhenChangeTypeNull() {
      assertThatThrownBy(() -> service.logToppingChange(topping, order, 5, null))
          .isInstanceOf(IllegalArgumentException.class);

      verifyNoInteractions(toppingLogRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0 })
    @DisplayName("Không ghi log khi số lượng thay đổi bằng 0 (log vô nghĩa)")
    void logChange_ThrowsException_WhenQuantityChangeIsZero(int quantity) {
      assertThatThrownBy(() -> service.logProductChange(product, order, quantity, ChangeType.IMPORT))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> service.logToppingChange(topping, order, quantity, ChangeType.IMPORT))
          .isInstanceOf(IllegalArgumentException.class);

      verifyNoInteractions(productLogRepository, toppingLogRepository);
    }
  }

  // ==========================================
  // 4. getProductLogs Tests
  // ==========================================
  @Nested
  @DisplayName("getProductLogs tests")
  class GetProductLogsTests {

    @Test
    @DisplayName("Map đúng field và giữ nguyên thứ tự (mới nhất trước)")
    void getProductLogs_MapsFieldsAndKeepsOrder() {
      LocalDateTime t1 = LocalDateTime.of(2026, 11, 2, 10, 0);
      LocalDateTime t2 = LocalDateTime.of(2026, 11, 2, 9, 0);
      ProductLog newer = ProductLog.builder()
          .id(2L).product(product).quantityChange(-3).changeType(ChangeType.ORDER_DEDUCT).createdAt(t1).build();
      ProductLog older = ProductLog.builder()
          .id(1L).product(product).quantityChange(10).changeType(ChangeType.IMPORT).createdAt(t2).build();
      when(productLogRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(newer, older));

      List<InventoryLogResponse> result = service.getProductLogs(1L);

      assertThat(result).hasSize(2);
      InventoryLogResponse first = result.get(0);
      assertThat(first.getId()).isEqualTo(2L);
      assertThat(first.getProductId()).isEqualTo(1L);
      assertThat(first.getProductName()).isEqualTo("Cà phê sữa");
      assertThat(first.getQuantityChange()).isEqualTo(-3);
      assertThat(first.getChangeType()).isEqualTo("ORDER_DEDUCT");
      assertThat(first.getCreatedAt()).isEqualTo(t1);
      assertThat(first.getToppingId()).isNull();
      assertThat(first.getToppingName()).isNull();
      assertThat(result.get(1).getId()).isEqualTo(1L);
      assertThat(result.get(1).getChangeType()).isEqualTo("IMPORT");
    }

    @Test
    @DisplayName("Chưa có log: trả về danh sách rỗng")
    void getProductLogs_ReturnsEmptyList_WhenNoLogs() {
      when(productLogRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

      assertThat(service.getProductLogs(1L)).isEmpty();
    }

    @Test
    @DisplayName("Log có kèm order vẫn map bình thường")
    void getProductLogs_MapsNormally_WhenLogHasOrder() {
      ProductLog log = ProductLog.builder()
          .id(3L).product(product).order(order).quantityChange(-2)
          .changeType(ChangeType.ORDER_DEDUCT).createdAt(LocalDateTime.of(2026, 11, 3, 8, 0)).build();
      when(productLogRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(log));

      List<InventoryLogResponse> result = service.getProductLogs(1L);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getId()).isEqualTo(3L);
      assertThat(result.get(0).getQuantityChange()).isEqualTo(-2);
    }

    @Test
    @DisplayName("Chỉ truy vấn log sản phẩm, không đụng tới log topping")
    void getProductLogs_OnlyQueriesProductLogRepository() {
      when(productLogRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

      service.getProductLogs(1L);

      verify(productLogRepository, times(1)).findByProductIdOrderByCreatedAtDesc(1L);
      verifyNoInteractions(toppingLogRepository);
    }
  }

  // ==========================================
  // 5. getToppingLogs Tests
  // ==========================================
  @Nested
  @DisplayName("getToppingLogs tests")
  class GetToppingLogsTests {

    @Test
    @DisplayName("Map đúng field cho log topping")
    void getToppingLogs_MapsFields() {
      LocalDateTime t1 = LocalDateTime.of(2026, 11, 2, 10, 0);
      ToppingLog log = ToppingLog.builder()
          .id(5L).topping(topping).quantityChange(20).changeType(ChangeType.IMPORT).createdAt(t1).build();
      when(toppingLogRepository.findByToppingIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(log));

      List<InventoryLogResponse> result = service.getToppingLogs(2L);

      assertThat(result).hasSize(1);
      InventoryLogResponse r = result.get(0);
      assertThat(r.getId()).isEqualTo(5L);
      assertThat(r.getToppingId()).isEqualTo(2L);
      assertThat(r.getToppingName()).isEqualTo("Trân châu");
      assertThat(r.getQuantityChange()).isEqualTo(20);
      assertThat(r.getChangeType()).isEqualTo("IMPORT");
      assertThat(r.getCreatedAt()).isEqualTo(t1);
      assertThat(r.getProductId()).isNull();
      assertThat(r.getProductName()).isNull();
    }

    @Test
    @DisplayName("Nhiều log: giữ nguyên thứ tự mới nhất trước")
    void getToppingLogs_KeepsOrder_ForMultipleLogs() {
      ToppingLog newer = ToppingLog.builder()
          .id(6L).topping(topping).quantityChange(-4).changeType(ChangeType.ORDER_DEDUCT)
          .createdAt(LocalDateTime.of(2026, 11, 2, 11, 0)).build();
      ToppingLog older = ToppingLog.builder()
          .id(5L).topping(topping).quantityChange(20).changeType(ChangeType.IMPORT)
          .createdAt(LocalDateTime.of(2026, 11, 2, 10, 0)).build();
      when(toppingLogRepository.findByToppingIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(newer, older));

      List<InventoryLogResponse> result = service.getToppingLogs(2L);

      assertThat(result).extracting(InventoryLogResponse::getId).containsExactly(6L, 5L);
      assertThat(result.get(0).getChangeType()).isEqualTo("ORDER_DEDUCT");
    }

    @Test
    @DisplayName("Chưa có log: trả về danh sách rỗng")
    void getToppingLogs_ReturnsEmptyList_WhenNoLogs() {
      when(toppingLogRepository.findByToppingIdOrderByCreatedAtDesc(2L)).thenReturn(Collections.emptyList());

      assertThat(service.getToppingLogs(2L)).isEmpty();
    }

    @Test
    @DisplayName("Chỉ truy vấn log topping, không đụng tới log sản phẩm")
    void getToppingLogs_OnlyQueriesToppingLogRepository() {
      when(toppingLogRepository.findByToppingIdOrderByCreatedAtDesc(2L)).thenReturn(Collections.emptyList());

      service.getToppingLogs(2L);

      verify(toppingLogRepository, times(1)).findByToppingIdOrderByCreatedAtDesc(2L);
      verifyNoInteractions(productLogRepository);
    }
  }
}