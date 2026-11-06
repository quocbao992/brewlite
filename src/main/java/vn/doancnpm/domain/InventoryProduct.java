package vn.doancnpm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Mỗi sản phẩm chỉ có đúng 1 dòng tồn kho -> unique = true
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false, unique = true)
  private Product product;

  @Column(name = "stock", nullable = false)
  @Builder.Default
  private Integer stock = 0; // Số lượng đang bán (menu/quầy)

  @Column(name = "warehouse_stock", nullable = false)
  @Builder.Default
  private Integer warehouseStock = 100; // Số lượng tồn kho lưu trữ

  @Column(name = "min_threshold")
  @Builder.Default
  private Integer minThreshold = 5; // Cảnh báo khi tồn kho nhỏ hơn mức này
}
