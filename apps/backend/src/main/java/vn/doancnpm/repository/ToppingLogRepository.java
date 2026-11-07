package vn.doancnpm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.doancnpm.domain.ToppingLog;

@Repository
public interface ToppingLogRepository extends JpaRepository<ToppingLog, Long> {
    List<ToppingLog> findByToppingIdOrderByCreatedAtDesc(Long toppingId);
    List<ToppingLog> findAllByOrderByCreatedAtDesc();
}
