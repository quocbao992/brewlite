package vn.doancnpm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStatus(Boolean status);
    List<Category> findAllByOrderByDisplayOrderAsc();
    List<Category> findByStatusOrderByDisplayOrderAsc(Boolean status);
    List<Category> findByNameContainingIgnoreCaseOrderByDisplayOrderAsc(String keyword);
    List<Category> findByNameContainingIgnoreCaseAndStatusOrderByDisplayOrderAsc(String keyword, Boolean status);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByDisplayOrder(Long displayOrder);
    boolean existsByDisplayOrderAndIdNot(Long displayOrder, Long id);
}