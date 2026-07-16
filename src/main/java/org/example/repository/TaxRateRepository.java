package org.example.repository;

import org.example.model.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {

    @Override
    @Query("SELECT t FROM TaxRate t WHERE t.deleted = false")
    List<TaxRate> findAll();

    @Override
    @Query("SELECT count(t) FROM TaxRate t WHERE t.deleted = false")
    long count();

    @Override
    @Query("SELECT t FROM TaxRate t WHERE t.id = :id AND t.deleted = false")
    Optional<TaxRate> findById(@Param("id") Long id);

    @Query("SELECT t FROM TaxRate t WHERE t.defaultRate = true AND t.deleted = false")
    Optional<TaxRate> findByIsDefaultTrue();
}