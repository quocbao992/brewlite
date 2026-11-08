package vn.doancnpm.service;

import java.util.List;
import vn.doancnpm.domain.Order;
import vn.doancnpm.dto.Response.InventoryStockResponse;

public interface ToppingInventoryService {

    void deductStock(Long toppingId, Order order, int deductAmount);

    void addStock(Long toppingId, Order order, int addAmount, String reason);

    void addWarehouseStock(Long toppingId, int amount, String reason);

    void transferToSelling(Long toppingId, int amount);

    boolean checkStockAvailable(Long toppingId, int requiredAmount);

    InventoryStockResponse getStock(Long toppingId);

    List<InventoryStockResponse> getAllToppingStocks();
}
