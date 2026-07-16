package org.example.service;

import org.example.model.TaxRate;
import org.example.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxRateService {

    private final TaxRateRepository taxRateRepository;
    private final AuditService auditService;

    private static final String MODULE_NAME = "SYSTÉM";

    @Cacheable(value = "taxRates", key = "#root.methodName")
    public long count() {
        return taxRateRepository.count();
    }

    @Cacheable(value = "taxRates", key = "#root.methodName")
    public List<TaxRate> findAll() {
        return taxRateRepository.findAll();
    }

    @Cacheable(value = "taxRates")
    public TaxRate findById(Long id) {
        return taxRateRepository.findById(id).orElseThrow();
    }

    @Cacheable(value = "taxRates", key = "#root.methodName")
    public TaxRate getDefaultRate() {
        return taxRateRepository.findByIsDefaultTrue().orElse(null);
    }

    @CacheEvict(value = "taxRates", allEntries = true)
    @Transactional
    public void save(TaxRate taxRate) {
        boolean isNew = (taxRate.getId() == null);

        if (taxRate.isDefaultRate()) {
            taxRateRepository.findByIsDefaultTrue().ifPresent(currentDefault -> {
                if (currentDefault.getId() != null && !currentDefault.getId().equals(taxRate.getId())) {
                    currentDefault.setDefaultRate(false);
                    taxRateRepository.save(currentDefault);
                }
            });
        } else {
            if (taxRateRepository.findByIsDefaultTrue().isEmpty() ||
                    (taxRate.getId() != null && taxRateRepository.findByIsDefaultTrue().get().getId().equals(taxRate.getId()))) {
                taxRate.setDefaultRate(true);
            }
        }

        taxRateRepository.save(taxRate);

        String action = isNew ? "Nová daňová sazba" : "Úprava daňové sazby";
        auditService.log(MODULE_NAME, action, "Zpracována daňová sazba: " + taxRate.getName() + " s hodnotou " + taxRate.getRate() + "%.");
    }

    @CacheEvict(value = "taxRates", allEntries = true)
    @Transactional
    public void delete(Long id) {
        TaxRate rate = findById(id);
        if (rate.isDefaultRate()) {
            throw new IllegalStateException("Nelze smazat výchozí daňovou sazbu.");
        }

        String rateName = rate.getName();
        taxRateRepository.deleteById(id);

        auditService.log(MODULE_NAME, "Smazání daňové sazby", "Smazána daňová sazba: " + rateName + ".");
    }
}