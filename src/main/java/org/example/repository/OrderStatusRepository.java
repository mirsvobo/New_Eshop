package org.example.repository;

import org.example.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatus, Long> {
    List<OrderStatus> findAllByOrderByDisplayOrderAsc();

    List<OrderStatus> findAllByActiveTrueOrderByDisplayOrderAsc();

    Optional<OrderStatus> findByName(String name);
}