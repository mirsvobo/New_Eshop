package org.example.repository;

import org.example.model.Product;
import org.example.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {
    List<StockMovement> findAllByOrderByTimestampDesc();

    List<StockMovement> findByProductOrderByTimestampDesc(Product product);
}