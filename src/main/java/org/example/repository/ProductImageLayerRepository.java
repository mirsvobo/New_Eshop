package org.example.repository;

import org.example.model.LayerType;
import org.example.model.ProductImageLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageLayerRepository extends JpaRepository<ProductImageLayer, Long> {

    List<ProductImageLayer> findAllByProductIdOrderByTypeAscSortOrderAscOptionNameAsc(Long productId);

    List<ProductImageLayer> findAllByProductIdAndActiveTrueOrderByTypeAscSortOrderAscOptionNameAsc(Long productId);

    List<ProductImageLayer> findAllByProductIdAndTypeAndActiveTrueOrderBySortOrderAscOptionNameAsc(
            Long productId,
            LayerType type
    );

    Optional<ProductImageLayer> findByIdAndProductId(Long id, Long productId);

    Optional<ProductImageLayer> findFirstByProductIdAndTypeAndActiveTrueAndOptionNameIgnoreCase(
            Long productId,
            LayerType type,
            String optionName
    );

    boolean existsByProductIdAndTypeAndActiveTrue(Long productId, LayerType type);

    boolean existsByProductIdAndTypeAndOptionNameIgnoreCase(
            Long productId,
            LayerType type,
            String optionName
    );

    boolean existsByProductIdAndTypeAndOptionNameIgnoreCaseAndIdNot(
            Long productId,
            LayerType type,
            String optionName,
            Long id
    );
}
