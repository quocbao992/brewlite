package vn.doancnpm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
/*mp bỏ
import org.springframework.data.jpa.repository.Modifying; */
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.doancnpm.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByStatus(boolean status);
    List<Product> findByCategoryIdAndStatus(Long categoryId, boolean status);
    @Query("""
            select p from Product p
            where (:categoryId is null or p.category.id = :categoryId)
              and (:keyword is null or lower(p.name) like lower(concat('%', cast(:keyword as string), '%')))
              and (:status is null or p.status = :status)
            """)
    Page<Product> search(@Param("categoryId") Long categoryId,
                         @Param("keyword") String keyword,
                         @Param("status") Boolean status,
                         Pageable pageable);
 
    List<Product> findByStatusOrderByIdDesc(boolean status, Pageable pageable);

    @Query("""
            select p from Product p
            where p.status = true and p.category.status = true
            order by p.category.displayOrder asc, p.name asc
            """)
    List<Product> findMenu(); 
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByImgURL(String imgURL);
    boolean existsByImgURLAndIdNot(String imgURL, Long id);
    boolean existsByCategoryId(Long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /*mp bỏ 
    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);
    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);
    */
}


