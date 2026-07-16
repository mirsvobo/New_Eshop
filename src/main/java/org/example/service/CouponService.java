package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.model.Coupon;
import org.example.repository.CouponRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final AuditService auditService;

    private static final String MODULE_NAME = "OBJEDNÁVKY";

    @Cacheable(value = "coupons", key = "#root.methodName")
    public List<Coupon> findAll() {
        return couponRepository.findAll();
    }

    @Cacheable(value = "coupons")
    public Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kupón s ID " + id + " neexistuje."));
    }

    @Cacheable(value = "coupons")
    public Optional<Coupon> findByCode(String code) {
        return couponRepository.findByCode(code.trim().toUpperCase());
    }

    @CacheEvict(value = "coupons", allEntries = true)
    @Transactional
    public void save(Coupon coupon) {
        boolean isNew = (coupon.getId() == null);
        coupon.setCode(coupon.getCode().trim().toUpperCase());
        couponRepository.save(coupon);

        String action = isNew ? "Nový kupón" : "Úprava kupónu";
        auditService.log(MODULE_NAME, action, "Zpracován slevový kupón s kódem: " + coupon.getCode() + ".");
    }

    @CacheEvict(value = "coupons", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Coupon coupon = findById(id);
        String code = coupon.getCode();
        couponRepository.deleteById(id);

        auditService.log(MODULE_NAME, "Smazání kupónu", "Byl smazán slevový kupón s kódem: " + code + ".");
    }

    @Cacheable(value = "coupons", key = "#root.methodName")
    public long countActive() {
        return couponRepository.countActiveAt(LocalDateTime.now());
    }

    public boolean isValid(Coupon coupon) {
        if (!coupon.isActive()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            return false;
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            return false;
        }
        return true;
    }
}