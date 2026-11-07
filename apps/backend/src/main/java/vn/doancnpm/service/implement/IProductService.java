package vn.doancnpm.service.implement;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import vn.doancnpm.domain.Category;
import vn.doancnpm.domain.Product;
import vn.doancnpm.dto.Request.ProductRequest;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.dto.Response.ProductResponse;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.service.CategoryService;
import vn.doancnpm.service.ProductInventoryService;
import vn.doancnpm.service.ProductService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IProductService implements ProductService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_IMG_URL_LENGTH = 255;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_FEATURED_LIMIT = 6;
    private static final int MAX_FEATURED_LIMIT = 50;
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000000");

    private final ProductRepository productRepository;
    private final ProductInventoryService productInventoryService;
    private final CategoryService categoryService;

    private ProductResponse convertToResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                category.getId(),
                category.getName(),
                product.getName(),
                product.getImgURL(),
                // Tồn kho thật nằm ở inventory_product, không đọc Product.stock nữa
                productInventoryService.getAvailableStock(product.getId()),
                product.getUnitPrice(),
                product.isStatus(),
                product.getVersion());
    }

    private List<ProductResponse> convertToResponses(List<Product> products) {
        return products.stream().map(this::convertToResponse).toList();
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy sản phẩm với ID: " + productId));
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá sản phẩm phải lớn hơn 0");
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Giá sản phẩm chỉ được có tối đa 2 chữ số thập phân");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Giá sản phẩm không được vượt quá 1.000.000.000");
        }
        return price;
    }

    private String validateName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        String name = rawName.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Tên sản phẩm không được dài quá " + MAX_NAME_LENGTH + " ký tự");
        }
        return name;
    }

    private String validateImgUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String url = rawUrl.trim();
        if (url.length() > MAX_IMG_URL_LENGTH) {
            throw new IllegalArgumentException(
                    "Đường dẫn ảnh không được dài quá " + MAX_IMG_URL_LENGTH + " ký tự");
        }
        return url.isEmpty() ? null : url;
    }

    // stock = null nghĩa là "không gửi lên" nên được phép
    private void validateStock(Integer stock) {
        if (stock != null && stock < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được âm");
        }
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        String name = validateName(request.getName());
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Phải chọn danh mục cho sản phẩm");
        }
        BigDecimal price = validatePrice(request.getUnitPrice());
        //validateStock(request.getStock());
        String imgURL = validateImgUrl(request.getImgURL());

        if (productRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại");
        }

        Product product = new Product();
        product.setName(name);
        product.setImgURL(imgURL);
        product.setUnitPrice(price);
        product.setStatus(request.getStatus() != null ? request.getStatus() : true);
        product.setCategory(categoryService.getActiveEntityById(request.getCategoryId()));

        Product saved = productRepository.save(product);

        // Tạo dòng tồn kho trong inventory_product (ghi log nếu có tồn đầu kỳ > 0)
        //int initialStock = request.getStock() == null ? 0 : request.getStock();
        productInventoryService.setStock(saved.getId(), 0, "INITIAL_STOCK");

        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        Product product = findProductOrThrow(productId);

        if (request.getVersion() != null && !request.getVersion().equals(product.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Product.class, productId);
        }

        // Chỉ sửa field nào có gửi lên (khác null)
        if (request.getName() != null) {
            String name = validateName(request.getName());
            if (productRepository.existsByNameIgnoreCaseAndIdNot(name, productId)) {
                throw new IllegalArgumentException("Tên sản phẩm đã tồn tại");
            }
            product.setName(name);
        }

        if (request.getCategoryId() != null
                && !product.getCategory().getId().equals(request.getCategoryId())) {
            product.setCategory(categoryService.getActiveEntityById(request.getCategoryId()));
        }

        if (request.getUnitPrice() != null) {
            product.setUnitPrice(validatePrice(request.getUnitPrice()));
        }

        if (request.getImgURL() != null) {
            product.setImgURL(validateImgUrl(request.getImgURL()));
        }

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        //validateStock(request.getStock());

        Product saved = productRepository.save(product);

        // Chỉ chỉnh tồn kho khi request có gửi stock
        //if (request.getStock() != null) {
        //    productInventoryService.setStock(productId, request.getStock(), "ADMIN_UPDATE");
        //}

        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updatePrice(Long productId, BigDecimal newPrice) {
        validatePrice(newPrice);
        Product product = findProductOrThrow(productId);
        product.setUnitPrice(newPrice);
        return convertToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateStatus(Long productId, boolean status) {
        Product product = findProductOrThrow(productId);
        product.setStatus(status);
        return convertToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long productId, int stock) {
        validateStock(stock);
        Product product = findProductOrThrow(productId);
        productInventoryService.setStock(productId, stock, "ADMIN_ADJUST");
        return convertToResponse(product);
    }

    // Xóa mềm: chỉ ẩn sản phẩm, giữ lại dữ liệu để đơn hàng cũ vẫn tra được
    @Override
    @Transactional
    public void delete(Long productId) {
        Product product = findProductOrThrow(productId);
        product.setStatus(false);
        productRepository.save(product);
    }

    // ===================== Query =====================

    @Override
    public ProductResponse getById(Long productId) {
        return convertToResponse(findProductOrThrow(productId));
    }

    @Override
    public PageResponse<ProductResponse> search(Long categoryId, String keyword, Boolean status,
                                                int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").ascending());

        String key = (keyword == null || keyword.trim().isEmpty())
                ? null
                : keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        Page<Product> result = productRepository.search(categoryId, key, status, pageable);

        return new PageResponse<>(
                convertToResponses(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public List<ProductResponse> getByCategory(Long categoryId, Boolean status) {
        if (!categoryService.existsById(categoryId)) {
            throw new EntityNotFoundException("Không tìm thấy danh mục với ID: " + categoryId);
        }
        Pageable all = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name").ascending());
        return convertToResponses(
                productRepository.search(categoryId, null, status, all).getContent());
    }

    @Override
    public List<ProductResponse> getFeatured(int limit) {
        int safeLimit = (limit <= 0) ? DEFAULT_FEATURED_LIMIT : Math.min(limit, MAX_FEATURED_LIMIT);
        return convertToResponses(
                productRepository.findByStatusOrderByIdDesc(true, PageRequest.of(0, safeLimit)));
    }

    @Override
    public List<ProductResponse> getMenu() {
        return convertToResponses(productRepository.findMenu());
    }

    @Override
    public boolean existsById(Long productId) {
        return productRepository.existsById(productId);
    }

    @Override
    public boolean isActive(Long productId) {
        return findProductOrThrow(productId).isStatus();
    }

    @Override
    public Product getEntityById(Long productId) {
        return findProductOrThrow(productId);
    }

    @Override
    public Product getActiveEntityById(Long productId) {
        Product product = findProductOrThrow(productId);
        if (!product.isStatus()) {
            throw new IllegalArgumentException(
                    "Sản phẩm \"" + product.getName() + "\" đã ngừng bán");
        }
        return product;
    }

    @Override
    public BigDecimal getUnitPrice(Long productId) {
        return getActiveEntityById(productId).getUnitPrice();
    }
}