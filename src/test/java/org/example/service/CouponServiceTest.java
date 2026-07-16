package org.example.service;

import org.example.model.Coupon;
import org.example.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CouponService couponService;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testCoupon = new Coupon();
        testCoupon.setId(1L);
        testCoupon.setCode("DISCOUNT20");
        testCoupon.setActive(true);
    }

    @Test
    void shouldSaveCouponWithUpperCaseCode() {
        // given
        testCoupon.setCode("  lowercase-code  ");

        // when
        couponService.save(testCoupon);

        // then
        assertThat(testCoupon.getCode()).isEqualTo("LOWERCASE-CODE");
        verify(couponRepository).save(testCoupon);
    }

    @Test
    void shouldFindCouponByCodeWhenCodeExists() {
        // given
        when(couponRepository.findByCode("DISCOUNT20")).thenReturn(Optional.of(testCoupon));

        // when
        Optional<Coupon> result = couponService.findByCode(" discount20 ");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("DISCOUNT20");
    }

    @Test
    void shouldReturnTrueWhenCouponIsValidAndWithinDateRange() {
        // given
        testCoupon.setValidFrom(LocalDateTime.now().minusDays(1));
        testCoupon.setValidUntil(LocalDateTime.now().plusDays(1));

        // when
        boolean result = couponService.isValid(testCoupon);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCouponIsExpired() {
        // given
        testCoupon.setValidUntil(LocalDateTime.now().minusMinutes(1));

        // when
        boolean result = couponService.isValid(testCoupon);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenCouponIsNotYetActive() {
        // given
        testCoupon.setValidFrom(LocalDateTime.now().plusDays(1));

        // when
        boolean result = couponService.isValid(testCoupon);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenCouponIsInactive() {
        // given
        testCoupon.setActive(false);

        // when
        boolean result = couponService.isValid(testCoupon);

        // then
        assertThat(result).isFalse();
    }
}