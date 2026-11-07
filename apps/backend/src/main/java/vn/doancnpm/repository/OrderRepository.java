package vn.doancnpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import vn.doancnpm.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


@Repository 


public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByStatus(Order.Status status);
    List<Order> findByCustomerIdAndStatus(Long customerId, Order.Status status);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    Page<Order> findByCustomerIdAndStatus(Long customerId, Order.Status status, Pageable pageable);
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByStatusInOrderByCreatedAtAsc(Collection<Order.Status> statuses);

}
