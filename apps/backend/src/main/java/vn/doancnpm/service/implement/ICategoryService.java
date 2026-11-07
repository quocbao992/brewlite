package vn.doancnpm.service.implement;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import vn.doancnpm.domain.Category;
import vn.doancnpm.dto.Request.CategoryRequest;
import vn.doancnpm.dto.Response.CategoryResponse;
import vn.doancnpm.repository.CategoryRepository;
import vn.doancnpm.repository.ProductRepository;
import vn.doancnpm.service.CategoryService;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ICategoryService implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    @Value("${category.allow-duplicate-display-order:true}")
    private boolean allowDuplicateDisplayOrder;

    private CategoryResponse convertToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getStatus(),
                category.getDisplayOrder());
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy danh mục với ID: " + categoryId));
    }

        private String validateName(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên danh mục không được để trống");
            }
            String trimmed = name.trim();
            if (trimmed.length() > 255) {
                throw new IllegalArgumentException("Tên danh mục không được vượt quá 255 ký tự");
            }
            return trimmed;
        }

        private void validateDisplayOrder(Long displayOrder) {
            if (displayOrder != null && displayOrder < 0) {
                throw new IllegalArgumentException("Thứ tự hiển thị không được âm");
            }
        }
        private void checkDuplicateDisplayOrder(Long displayOrder, Long excludeId) {
        if (allowDuplicateDisplayOrder || displayOrder == null) {
            return;
        }
        boolean exists = (excludeId == null)
                ? categoryRepository.existsByDisplayOrder(displayOrder)
                : categoryRepository.existsByDisplayOrderAndIdNot(displayOrder, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Thứ tự hiển thị " + displayOrder + " đã được dùng");
        }
    }  
    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = validateName(request.getName());
        validateDisplayOrder(request.getDisplayOrder());

        Long displayOrder = request.getDisplayOrder() != null ? request.getDisplayOrder() : 0L;
        checkDuplicateDisplayOrder(displayOrder, null);

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        Category category = Category.builder()
                .name(name)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0L)
                .status(request.getStatus() != null ? request.getStatus() : true)
                .build();

        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long categoryId, CategoryRequest request) {
        Category category = findCategoryOrThrow(categoryId);
        validateDisplayOrder(request.getDisplayOrder());
        checkDuplicateDisplayOrder(request.getDisplayOrder(), categoryId);

        if (request.getName() != null) {
            String name = validateName(request.getName());
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
                throw new IllegalArgumentException("Tên danh mục đã tồn tại");
            }
            category.setName(name);
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse hide(Long categoryId) {
        Category category = findCategoryOrThrow(categoryId);
        category.setStatus(false);
        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse show(Long categoryId) {
        Category category = findCategoryOrThrow(categoryId);
        category.setStatus(true);
        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse getById(Long categoryId) {
        return convertToResponse(findCategoryOrThrow(categoryId));
    }

    @Override
    public List<CategoryResponse> getAll(Boolean status) {
        List<Category> categories = (status == null)
                ? categoryRepository.findAllByOrderByDisplayOrderAsc()
                : categoryRepository.findByStatusOrderByDisplayOrderAsc(status);

        return categories.stream().map(this::convertToResponse).toList();
    }

    @Override
    public List<CategoryResponse> getActiveForMenu() {
        return categoryRepository.findByStatusOrderByDisplayOrderAsc(true)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> searchByName(String keyword, Boolean status) {
        String key = (keyword == null) ? "" : keyword.trim();

        List<Category> categories = (status == null)
                ? categoryRepository.findByNameContainingIgnoreCaseOrderByDisplayOrderAsc(key)
                : categoryRepository.findByNameContainingIgnoreCaseAndStatusOrderByDisplayOrderAsc(key, status);

        return categories.stream().map(this::convertToResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse updateDisplayOrder(Long categoryId, Long newDisplayOrder) {
        if (newDisplayOrder == null || newDisplayOrder < 0) {
            throw new IllegalArgumentException("Thứ tự hiển thị không hợp lệ");
        }
        Category category = findCategoryOrThrow(categoryId);
        checkDuplicateDisplayOrder(newDisplayOrder, categoryId);
        category.setDisplayOrder(newDisplayOrder);
        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        Category category = findCategoryOrThrow(categoryId);
        if (productRepository.existsByCategoryId(categoryId)) {
            throw new IllegalArgumentException(
                    "Danh mục \"" + category.getName() + "\" còn sản phẩm nên không thể xóa. "
                            + "Hãy ẩn danh mục hoặc chuyển sản phẩm sang danh mục khác");
        }
        categoryRepository.delete(category);
    }

    // ===== Các hàm dùng chung cho module khác =====

    @Override
    public boolean existsById(Long categoryId) {
        return categoryRepository.existsById(categoryId);
    }

    @Override
    public boolean isActive(Long categoryId) {
        return Boolean.TRUE.equals(findCategoryOrThrow(categoryId).getStatus());
    }

    @Override
    public Category getEntityById(Long categoryId) {
        return findCategoryOrThrow(categoryId);
    }

    @Override
    public Category getActiveEntityById(Long categoryId) {
        Category category = findCategoryOrThrow(categoryId);
        if (!Boolean.TRUE.equals(category.getStatus())) {
            throw new IllegalArgumentException(
                    "Danh mục \"" + category.getName() + "\" đang bị ẩn");
        }
        return category;
    }
}