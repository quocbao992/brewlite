package vn.doancnpm.service;

import java.util.List;
import vn.doancnpm.domain.Order;
import vn.doancnpm.dto.Response.InventoryStockResponse;

public interface ProductInventoryService {

    // Trừ số lượng đang bán (khi khách chốt đơn)
    void deductStock(Long productId, Order order, int deductAmount);

    // Cộng số lượng đang bán trực tiếp
    void addStock(Long productId, Order order, int addAmount, String reason);

    // Thêm số lượng tồn kho (nhập hàng kho lưu trữ)
    void addWarehouseStock(Long productId, int amount, String reason);

    // Chuyển từ tồn kho sang đang bán: trừ tồn kho, cộng đang bán
    void transferToSelling(Long productId, int amount);

    // Kiểm tra còn đủ hàng đang bán không (trước khi thanh toán)
    boolean checkStockAvailable(Long productId, int requiredAmount);

    InventoryStockResponse getStock(Long productId);

    List<InventoryStockResponse> getAllProductStocks();

    int getAvailableStock(Long productId);

    void setStock(Long productId, int newStock, String reason);

    void initInventory(Long productId, int initialWarehouseStock);
}
