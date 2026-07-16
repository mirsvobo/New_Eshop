package org.example.repository;

import org.example.model.Order;
import org.example.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @Query("SELECT o FROM Order o WHERE o.isDeleted = false")
    List<Order> findAll();

    @Override
    @Query("SELECT o FROM Order o WHERE o.isDeleted = false")
    List<Order> findAll(Sort sort);

    @Override
    @Query("SELECT count(o) FROM Order o WHERE o.isDeleted = false")
    long count();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.isDeleted = false")
    BigDecimal sumTotalRevenue();

    @Override
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.isDeleted = false")
    Optional<Order> findById(@Param("id") Long id);

    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.isDeleted = false ORDER BY o.createdAt DESC")
    List<Order> findByCustomerOrderByCreatedAtDesc(@Param("customer") User customer);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customer = :customer AND o.isDeleted = false")
    BigDecimal sumTotalAmountByCustomer(@Param("customer") User customer);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN o.customer c " +
            "WHERE o.isDeleted = false " +
            "AND (:statusId IS NULL OR o.status.id = :statusId) " +
            "AND (:search IS NULL OR :search = '' " +
            "  OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(o.guestLastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(o.guestEmail) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Order> findFilteredOrders(@Param("statusId") Long statusId, @Param("search") String search, Sort sort);
}