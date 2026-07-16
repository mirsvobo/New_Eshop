package org.example.repository;

import org.example.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndActiveTrue(String code);

    Optional<Coupon> findByCode(String code);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.active = true " +
            "AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
            "AND (c.validUntil IS NULL OR c.validUntil >= :now)")
    long countActiveAt(@Param("now") LocalDateTime now);
}