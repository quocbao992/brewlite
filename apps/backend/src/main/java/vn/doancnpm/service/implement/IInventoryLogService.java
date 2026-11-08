package vn.doancnpm.service.implement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Product;
import vn.doancnpm.domain.ProductLog;
import vn.doancnpm.domain.Topping;
import vn.doancnpm.domain.ToppingLog;
import vn.doancnpm.dto.Response.InventoryLogResponse;
import vn.doancnpm.repository.ProductLogRepository;
import vn.doancnpm.repository.ToppingLogRepository;
import vn.doancnpm.service.InventoryLogService;

@Service
@Transactional(readOnly = true)
public class IInventoryLogService implements InventoryLogService {

    private final ProductLogRepository productLogRepository;
    private final ToppingLogRepository toppingLogRepository;

    public IInventoryLogService(
            ProductLogRepository productLogRepository,
            ToppingLogRepository toppingLogRepository) {
        this.productLogRepository = productLogRepository;
        this.toppingLogRepository = toppingLogRepository;
    }

    @Override
    @Transactional
    public void logProductChange(Product product, Order order, int quantityChange, String changeType) {
        ProductLog log = new ProductLog();
        log.setProduct(product);
        log.setOrder(order);
        log.setQuantityChange(quantityChange);
        log.setChangeType(changeType);
        log.setCreatedAt(LocalDateTime.now());
        productLogRepository.save(log);
    }

    @Override
    @Transactional
    public void logToppingChange(Topping topping, Order order, int quantityChange, String changeType) {
        ToppingLog log = new ToppingLog();
        log.setTopping(topping);
        log.setOrder(order);
        log.setQuantityChange(quantityChange);
        log.setChangeType(changeType);
        log.setCreatedAt(LocalDateTime.now());
        toppingLogRepository.save(log);
    }

    @Override
    public List<InventoryLogResponse> getProductLogs(Long productId) {
        List<ProductLog> logs = productLogRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return logs.stream().map(this::mapProductLog).collect(Collectors.toList());
    }

    @Override
    public List<InventoryLogResponse> getToppingLogs(Long toppingId) {
        List<ToppingLog> logs = toppingLogRepository.findByToppingIdOrderByCreatedAtDesc(toppingId);
        return logs.stream().map(this::mapToppingLog).collect(Collectors.toList());
    }

    @Override
    public List<InventoryLogResponse> getAllLogs() {
        List<InventoryLogResponse> all = new ArrayList<>();
        List<ProductLog> pLogs = productLogRepository.findAllByOrderByCreatedAtDesc();
        if (pLogs != null) {
            for (ProductLog pl : pLogs) {
                all.add(mapProductLog(pl));
            }
        }
        List<ToppingLog> tLogs = toppingLogRepository.findAllByOrderByCreatedAtDesc();
        if (tLogs != null) {
            for (ToppingLog tl : tLogs) {
                all.add(mapToppingLog(tl));
            }
        }

        all.sort(Comparator.comparing(InventoryLogResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return all;
    }

    private InventoryLogResponse mapProductLog(ProductLog log) {
        InventoryLogResponse resp = new InventoryLogResponse();
        resp.setId(log.getId());
        resp.setProductId(log.getProduct() != null ? log.getProduct().getId() : null);
        resp.setProductName(log.getProduct() != null ? log.getProduct().getName() : "");
        resp.setQuantityChange(log.getQuantityChange());
        resp.setChangeType(log.getChangeType());
        resp.setCreatedAt(log.getCreatedAt());
        return resp;
    }

    private InventoryLogResponse mapToppingLog(ToppingLog log) {
        InventoryLogResponse resp = new InventoryLogResponse();
        resp.setId(log.getId());
        resp.setToppingId(log.getTopping() != null ? log.getTopping().getId() : null);
        resp.setToppingName(log.getTopping() != null ? log.getTopping().getName() : "");
        resp.setQuantityChange(log.getQuantityChange());
        resp.setChangeType(log.getChangeType());
        resp.setCreatedAt(log.getCreatedAt());
        return resp;
    }
}
