package vn.doancnpm.service;
 
import java.util.List;
import vn.doancnpm.dto.Request.CategoryRequest;
import vn.doancnpm.dto.Response.CategoryResponse;
import vn.doancnpm.domain.Category;
 
public interface CategoryService {
 
    CategoryResponse create(CategoryRequest request); //them 
    CategoryResponse update(Long categoryId, CategoryRequest request); //sua thong tin
    CategoryResponse hide(Long categoryId); // an
    CategoryResponse show(Long categoryId); // hien
    CategoryResponse getById(Long categoryId); // xem chi tiet
    List<CategoryResponse> getAll(Boolean status);
    List<CategoryResponse> getActiveForMenu();
    List<CategoryResponse> searchByName(String keyword, Boolean status);
    CategoryResponse updateDisplayOrder(Long categoryId, Long newDisplayOrder);
    void delete(Long categoryId);
    boolean existsById(Long categoryId);
    boolean isActive(Long categoryId);
    Category getEntityById(Long categoryId);
    Category getActiveEntityById(Long categoryId);
}
 