package vn.doancnpm.service.implement;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import vn.doancnpm.domain.InventoryProduct;
import vn.doancnpm.domain.Order;
import vn.doancnpm.domain.Product;
import vn.doancnpm.dto.Response.InventoryStockResponse;
import vn.doancnpm.repository.InventoryProductRepository;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.service.InventoryLogService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.domain.InventoryLog.ChangeType;

@Service
@Transactional(readOnly = true)
public class IProductInventoryService implements ProductInventoryService {

    private final ProductRepository productRepository;
    private final InventoryProductRepository inventoryProductRepository;
    private final InventoryLogService inventoryLogService;

    public IProductInventoryService(
            ProductRepository productRepository,
            InventoryProductRepository inventoryProductRepository,
            InventoryLogService inventoryLogService) {
        this.productRepository = productRepository;
        this.inventoryProductRepository = inventoryProductRepository;
        this.inventoryLogService = inventoryLogService;
    }

    @Override
    @Transactional
    public void deductStock(Long productId, Order order, int deductAmount) {
        if (deductAmount <= 0) {
            throw new IllegalArgumentException("Số lượng trừ phải lớn hơn 0");
        }

        Product product = findProductOrThrow(productId);
        if (!product.isStatus()) {
            throw new IllegalArgumentException(
                    "Sản phẩm \"" + product.getName() + "\" đã ngừng bán");
        }

        InventoryProduct invProduct = inventoryProductRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Chưa thiết lập kho cho sản phẩm ID: " + productId
                                + " (hãy nhập kho trước)"));

        if (invProduct.getStock() < deductAmount) {
            throw new IllegalArgumentException("Sản phẩm không đủ số lượng đang bán!");
        }

        invProduct.setStock(invProduct.getStock() - deductAmount);
        inventoryProductRepository.save(invProduct);
        inventoryLogService.logProductChange(product, order, -deductAmount, ChangeType.ORDER_DEDUCT);
    }

    @Override
    @Transactional
    public void addStock(Long productId,Order order, int addAmount, String reason) {
        if (addAmount <= 0) {
            throw new IllegalArgumentException("Số lượng thêm phải lớn hơn 0");
        }

        Product product = findProductOrThrow(productId);

        InventoryProduct invProduct = inventoryProductRepository.findByProductId(productId)
                .orElseGet(() -> {
                    InventoryProduct p = new InventoryProduct();
                    p.setProduct(product);
                    p.setStock(0);
                    p.setWarehouseStock(100);
                    p.setMinThreshold(5);
                    return p;
                });

        invProduct.setStock(invProduct.getStock() + addAmount);
        inventoryProductRepository.save(invProduct);
        inventoryLogService.logProductChange(product, null, addAmount,ChangeType.IMPORT 
        ,reason
        //reason != null ? reason : "THÊM ĐANG BÁN"
        );
    }

    @Override
    @Transactional
    public void addWarehouseStock(Long productId, int amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng nhập tồn kho phải lớn hơn 0");
        }

        Product product = findProductOrThrow(productId);

        InventoryProduct invProduct = inventoryProductRepository.findByProductId(productId)
                .orElseGet(() -> {
                    InventoryProduct p = new InventoryProduct();
                    p.setProduct(product);
                    p.setStock(0);
                    p.setWarehouseStock(0);
                    p.setMinThreshold(5);
                    return p;
                });

        int currentWh = invProduct.getWarehouseStock() != null ? invProduct.getWarehouseStock() : 0;
        invProduct.setWarehouseStock(currentWh + amount);
        inventoryProductRepository.save(invProduct);
        inventoryLogService.logProductChange(product, null, amount, ChangeType.IMPORT, reason != null && !reason.isBlank() ? reason : "NHẬP KHO"
        );
    }

    @Override
    @Transactional
    public void transferToSelling(Long productId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng chuyển sang bán phải lớn hơn 0");
        }

        Product product = findProductOrThrow(productId);

        InventoryProduct invProduct = inventoryProductRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Sản phẩm chưa có bản ghi tồn kho: " + productId));

        int currentWh = invProduct.getWarehouseStock() != null ? invProduct.getWarehouseStock() : 0;
        if (currentWh < amount) {
            throw new IllegalArgumentException("Số lượng tồn kho không đủ để chuyển sang bán! (Hiện có: " + currentWh
                    + ", yêu cầu: " + amount + ")");
        }

        invProduct.setWarehouseStock(currentWh - amount);
        invProduct.setStock((invProduct.getStock() != null ? invProduct.getStock() : 0) + amount);
        inventoryProductRepository.save(invProduct);

        inventoryLogService.logProductChange(product, null, amount,ChangeType.TRANSFER,"CHUYỂN SANG BÁN (Kho -> Quầy)"
         );
    }

    @Override
    public boolean checkStockAvailable(Long productId, int requiredAmount) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || !product.isStatus()) {
            return false;
        }
        return inventoryProductRepository.findByProductId(productId)
                .map(inv -> inv.getStock() != null && inv.getStock() >= requiredAmount)
                .orElse(false);
    }

    @Override
    public InventoryStockResponse getStock(Long productId) {
        InventoryProduct inv = inventoryProductRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy tồn kho cho Product ID: " + productId));
        return mapToStockResponse(inv);
    }

    @Override
    public List<InventoryStockResponse> getAllProductStocks() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(p -> {
            InventoryProduct inv = inventoryProductRepository.findByProductId(p.getId())
                    .orElseGet(() -> {
                        InventoryProduct created = new InventoryProduct();
                        created.setProduct(p);
                        created.setStock(0);
                        created.setWarehouseStock(100);
                        created.setMinThreshold(5);
                        return inventoryProductRepository.save(created);
                    });
            return mapToStockResponse(inv);
        }).collect(Collectors.toList());
    }

    private InventoryStockResponse mapToStockResponse(InventoryProduct inv) {
        InventoryStockResponse resp = new InventoryStockResponse();
        resp.setInventoryId(inv.getId());
        resp.setItemId(inv.getProduct() != null ? inv.getProduct().getId() : null);
        resp.setItemName(inv.getProduct() != null ? inv.getProduct().getName() : "");
        resp.setItemType("PRODUCT");
        resp.setStock(inv.getStock() != null ? inv.getStock() : 0);
        resp.setWarehouseStock(inv.getWarehouseStock() != null ? inv.getWarehouseStock() : 0);
        resp.setMinThreshold(inv.getMinThreshold() != null ? inv.getMinThreshold() : 5);
        return resp;
    }

    @Override
    public int getAvailableStock(Long productId) {
        return inventoryProductRepository.findByProductId(productId)
                .map(inv -> inv.getStock() != null ? inv.getStock() : 0)
                .orElse(0);
    }

    @Override
    @Transactional
    public void setStock(Long productId, int stock, String reason) {
        
        InventoryProduct inv = inventoryProductRepository.findByProductId(productId)
                .orElseGet(() -> {
                    InventoryProduct newInv = new InventoryProduct();
                    newInv.setProduct(productRepository.getReferenceById(productId));
                    newInv.setMinThreshold(5);
                    newInv.setWarehouseStock(100);
                    newInv.setStock(0);
                    return newInv;
                });

        inv.setStock(stock);
        inventoryProductRepository.save(inv);
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy sản phẩm với ID: " + productId));
    }
}
