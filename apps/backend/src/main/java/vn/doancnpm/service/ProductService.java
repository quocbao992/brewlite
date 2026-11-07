package vn.doancnpm.service;

import java.math.BigDecimal;
import java.util.List;

import vn.doancnpm.domain.Product;
import vn.doancnpm.dto.Request.ProductRequest;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.dto.Response.ProductResponse;

public interface ProductService {

    ProductResponse create(ProductRequest request);
    ProductResponse update(Long productId, ProductRequest request);
    ProductResponse updatePrice(Long productId, BigDecimal newPrice);
    ProductResponse updateStatus(Long productId, boolean status);
    ProductResponse updateStock(Long productId, int stock);
    void delete(Long productId);

    ProductResponse getById(Long productId);
    PageResponse<ProductResponse> search(Long categoryId, String keyword, Boolean status, int page, int size);
    List<ProductResponse> getByCategory(Long categoryId, Boolean status);
    List<ProductResponse> getFeatured(int limit);
    List<ProductResponse> getMenu();

    boolean existsById(Long productId);
    boolean isActive(Long productId);
    Product getEntityById(Long productId);
    Product getActiveEntityById(Long productId);
    BigDecimal getUnitPrice(Long productId);
}