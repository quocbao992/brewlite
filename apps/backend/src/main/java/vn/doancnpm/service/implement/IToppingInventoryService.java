package vn.doancnpm.service.implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import vn.doancnpm.domain.InventoryTopping;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.dto.Response.InventoryStockResponse;
import vn.doancnpm.repository.InventoryToppingRepository;
import vn.doancnpm.repository.ToppingRepository;
import vn.doancnpm.service.InventoryLogService;
import vn.doancnpm.service.ToppingInventoryService;

@Service
@Transactional(readOnly = true)
public class IToppingInventoryService implements ToppingInventoryService {

    private final ToppingRepository toppingRepository;
    private final InventoryToppingRepository inventoryToppingRepository;
    private final InventoryLogService inventoryLogService;

    public IToppingInventoryService(
            ToppingRepository toppingRepository,
            InventoryToppingRepository inventoryToppingRepository,
            InventoryLogService inventoryLogService) {
        this.toppingRepository = toppingRepository;
        this.inventoryToppingRepository = inventoryToppingRepository;
        this.inventoryLogService = inventoryLogService;
    }

    @Override
    @Transactional
    public void deductStock(Long toppingId, Order order, int deductAmount) {
        if (deductAmount <= 0) {
            throw new IllegalArgumentException("Số lượng trừ phải lớn hơn 0");
        }

        Topping topping = findToppingOrThrow(toppingId);
        if (!Boolean.TRUE.equals(topping.getStatus())) {
            throw new IllegalArgumentException(
                    "Topping \"" + topping.getName() + "\" đã ngừng bán");
        }

        InventoryTopping invTopping = inventoryToppingRepository.findByToppingId(toppingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Chưa thiết lập kho cho topping ID: " + toppingId
                                + " (hãy nhập kho trước)"));

        if (invTopping.getStock() < deductAmount) {
            throw new IllegalArgumentException("Topping không đủ số lượng đang bán!");
        }

        invTopping.setStock(invTopping.getStock() - deductAmount);
        inventoryToppingRepository.save(invTopping);
        inventoryLogService.logToppingChange(topping, order, -deductAmount,"KHÁCH MUA TRỪ BÁN");
    }

    @Override
    @Transactional
    public void addStock(Long toppingId, Order order, int addAmount, String reason) {
        if (addAmount <= 0) {
            throw new IllegalArgumentException("Số lượng thêm phải lớn hơn 0");
        }

        Topping topping = findToppingOrThrow(toppingId);

        InventoryTopping invTopping = inventoryToppingRepository.findByToppingId(toppingId)
                .orElseGet(() -> {
                    InventoryTopping t = new InventoryTopping();
                    t.setTopping(topping);
                    t.setStock(0);
                    t.setWarehouseStock(200);
                    t.setMinThreshold(10);
                    return t;
                });

        invTopping.setStock(invTopping.getStock() + addAmount);
        inventoryToppingRepository.save(invTopping);
        inventoryLogService.logToppingChange(topping, order, addAmount,reason != null ? reason : "THÊM ĐANG BÁN");
    }

    @Override
    @Transactional
    public void addWarehouseStock(Long toppingId, int amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng nhập tồn kho phải lớn hơn 0");
        }

        Topping topping = findToppingOrThrow(toppingId);

        InventoryTopping invTopping = inventoryToppingRepository.findByToppingId(toppingId)
                .orElseGet(() -> {
                    InventoryTopping t = new InventoryTopping();
                    t.setTopping(topping);
                    t.setStock(0);
                    t.setWarehouseStock(0);
                    t.setMinThreshold(10);
                    return t;
                });

        int currentWh = invTopping.getWarehouseStock() != null ? invTopping.getWarehouseStock() : 0;
        invTopping.setWarehouseStock(currentWh + amount);
        inventoryToppingRepository.save(invTopping);
        inventoryLogService.logToppingChange(topping, null, amount,reason != null && !reason.isBlank() ? reason : "NHẬP TỒN KHO");
    }

    @Override
    @Transactional
    public void transferToSelling(Long toppingId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng chuyển sang bán phải lớn hơn 0");
        }

        Topping topping = findToppingOrThrow(toppingId);

        InventoryTopping invTopping = inventoryToppingRepository.findByToppingId(toppingId)
                .orElseThrow(() -> new EntityNotFoundException("Topping chưa có bản ghi tồn kho: " + toppingId));

        int currentWh = invTopping.getWarehouseStock() != null ? invTopping.getWarehouseStock() : 0;
        if (currentWh < amount) {
            throw new IllegalArgumentException("Số lượng tồn kho không đủ để chuyển sang bán! (Hiện có: " + currentWh + ", yêu cầu: " + amount + ")");
        }

        invTopping.setWarehouseStock(currentWh - amount);
        invTopping.setStock((invTopping.getStock() != null ? invTopping.getStock() : 0) + amount);
        inventoryToppingRepository.save(invTopping);

        inventoryLogService.logToppingChange(topping, null, amount, "CHUYỂN SANG BÁN (Kho -> Quầy)");
    }

    @Override
    public boolean checkStockAvailable(Long toppingId, int requiredAmount) {
        Topping topping = toppingRepository.findById(toppingId).orElse(null);
        if (topping == null || !Boolean.TRUE.equals(topping.getStatus())) {
            return false;
        }
        return inventoryToppingRepository.findByToppingId(toppingId)
                .map(inv -> inv.getStock() != null && inv.getStock() >= requiredAmount)
                .orElse(false);
    }

    @Override
    public InventoryStockResponse getStock(Long toppingId) {
        InventoryTopping inv = inventoryToppingRepository.findByToppingId(toppingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy tồn kho cho Topping ID: " + toppingId));
        return mapToStockResponse(inv);
    }

    @Override
    public List<InventoryStockResponse> getAllToppingStocks() {
        List<Topping> toppings = toppingRepository.findAll();
        return toppings.stream().map(t -> {
            InventoryTopping inv = inventoryToppingRepository.findByToppingId(t.getId())
                    .orElseGet(() -> {
                        InventoryTopping created = new InventoryTopping();
                        created.setTopping(t);
                        created.setStock(0);
                        created.setWarehouseStock(200);
                        created.setMinThreshold(10);
                        return inventoryToppingRepository.save(created);
                    });
            return mapToStockResponse(inv);
        }).collect(Collectors.toList());
    }

    private InventoryStockResponse mapToStockResponse(InventoryTopping inv) {
        InventoryStockResponse resp = new InventoryStockResponse();
        resp.setInventoryId(inv.getId());
        resp.setItemId(inv.getTopping() != null ? inv.getTopping().getId() : null);
        resp.setItemName(inv.getTopping() != null ? inv.getTopping().getName() : "");
        resp.setItemType("TOPPING");
        resp.setStock(inv.getStock() != null ? inv.getStock() : 0);
        resp.setWarehouseStock(inv.getWarehouseStock() != null ? inv.getWarehouseStock() : 0);
        resp.setMinThreshold(inv.getMinThreshold() != null ? inv.getMinThreshold() : 10);
        return resp;
    }

    private Topping findToppingOrThrow(Long toppingId) {
        return toppingRepository.findById(toppingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy topping với ID: " + toppingId));
    }
}
