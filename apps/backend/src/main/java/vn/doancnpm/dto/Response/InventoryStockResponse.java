package vn.doancnpm.dto.Response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryStockResponse {
    private Long inventoryId;
    private String inventoryName;
    private Long itemId;          // ID của Product hoặc Topping
    private String itemName;      // Tên của Product hoặc Topping
    private String itemType;      // "PRODUCT" hoặc "TOPPING"
    private Integer stock;        // Số lượng đang bán hiện tại (selling stock)
    private Integer warehouseStock; // Số lượng tồn kho lưu trữ (warehouse stock)
    private Integer minThreshold;

    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }

    public String getInventoryName() { return inventoryName; }
    public void setInventoryName(String inventoryName) { this.inventoryName = inventoryName; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getWarehouseStock() { return warehouseStock; }
    public void setWarehouseStock(Integer warehouseStock) { this.warehouseStock = warehouseStock; }

    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
}
