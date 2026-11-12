package vn.doancnpm.service;

import java.util.List;

import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.dto.Response.InventoryLogResponse;
import vn.doancnpm.domain.InventoryLog.ChangeType;

public interface InventoryLogService {

    void logProductChange(Product product, Order order, int quantityChange, ChangeType changeType, String reason);

    void logToppingChange(Topping topping, Order order, int quantityChange, ChangeType changeType, String reason);

    default void logProductChange(Product product, Order order, int quantityChange, ChangeType changeType) {
        logProductChange(product, order, quantityChange, changeType, null);
    }

    default void logToppingChange(Topping topping, Order order, int quantityChange, ChangeType changeType) {
        logToppingChange(topping, order, quantityChange, changeType, null);
    }

    List<InventoryLogResponse> getProductLogs(Long productId);

    List<InventoryLogResponse> getToppingLogs(Long toppingId);

    List<InventoryLogResponse> getAllLogs();
}
