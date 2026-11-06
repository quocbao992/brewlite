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
@Table(name = "inventory_topping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTopping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topping_id", nullable = false, unique = true)
    private Topping topping;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0; // Số lượng đang bán

    @Column(name = "warehouse_stock", nullable = false)
    @Builder.Default
    private Integer warehouseStock = 200; // Số lượng tồn kho lưu trữ

    @Column(name = "min_threshold")
    @Builder.Default
    private Integer minThreshold = 10;
}
