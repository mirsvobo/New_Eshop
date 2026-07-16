package org.example.repository;

import org.example.model.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false")
    List<Product> findAll();

    @Override
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false")
    List<Product> findAll(Sort sort);

    @Override
    @Query("SELECT count(p) FROM Product p WHERE p.isDeleted = false")
    long count();

    @Override
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Product> findById(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.isDeleted = false")
    List<Product> findByActiveTrue();

    // Optimalizace: Přidán parametr Sort pro řazení v DB
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.type = :type AND p.isDeleted = false")
    List<Product> findByActiveTrueAndType(@Param("type") Product.ProductType type, Sort sort);

    List<Product> findByTypeAndIsDeletedFalse(Product.ProductType type);

    @Query("SELECT p FROM Product p " +
            "WHERE p.isDeleted = false " +
            "AND (:search IS NULL OR :search = '' " +
            "  OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:type IS NULL OR p.type = :type) " +
            "AND (:active IS NULL OR p.active = :active)")
    List<Product> findFilteredProducts(@Param("search") String search,
                                       @Param("type") Product.ProductType type,
                                       @Param("active") Boolean active,
                                       Sort sort);
}