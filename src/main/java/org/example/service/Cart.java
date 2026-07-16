package org.example.service;

import lombok.Getter;
import org.example.dto.CartItemDto;
import org.example.model.Coupon;
import org.example.model.DiscountType;
import org.example.model.Product;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Component
@SessionScope
public class Cart {

    private final List<CartItemDto> items = new ArrayList<>();
    private Coupon appliedCoupon;

    public void addItem(CartItemDto item) {
        for (CartItemDto existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId())) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                // Aktualizujeme stav skladu podle nejnovější informace z DB
                existingItem.setStockQuantity(item.getStockQuantity());
                return;
            }
        }
        items.add(item);
    }

    public void updateQuantity(Long productId, Integer newQuantity) {
        if (newQuantity == null || newQuantity <= 0) {
            items.removeIf(item -> item.getProductId().equals(productId));
            return;
        }
        for (CartItemDto existingItem : items) {
            if (existingItem.getProductId().equals(productId)) {
                existingItem.setQuantity(newQuantity);
                return;
            }
        }
    }

    public void clear() {
        items.clear();
        appliedCoupon = null;
    }

    public void applyCoupon(Coupon coupon) {
        this.appliedCoupon = coupon;
    }

    public void removeCoupon() {
        this.appliedCoupon = null;
    }

    public int getTotalItems() {
        return items.stream().mapToInt(CartItemDto::getQuantity).sum();
    }

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalBasePrice() {
        return items.stream()
                .map(item -> item.getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal getDiscountAmount() {
        if (appliedCoupon == null) return BigDecimal.ZERO;

        Set<Long> applicableProductIds = appliedCoupon.getApplicableProducts()
                .stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        BigDecimal applicableTotal = items.stream()
                .filter(item -> applicableProductIds.isEmpty() || applicableProductIds.contains(item.getProductId()))
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (applicableTotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;
        if (appliedCoupon.getType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = appliedCoupon.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = applicableTotal.multiply(percentage);
        } else if (appliedCoupon.getType() == DiscountType.FIXED) {
            discount = appliedCoupon.getDiscountValue();
            if (discount.compareTo(applicableTotal) > 0) {
                discount = applicableTotal;
            }
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getFinalPrice() {
        BigDecimal total = getTotalPrice().subtract(getDiscountAmount());
        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    public Map<BigDecimal, BigDecimal> getTaxBreakdown() {
        return items.stream().collect(Collectors.groupingBy(
                CartItemDto::getTaxRateValue,
                Collectors.mapping(item -> {
                    BigDecimal lineBase = item.getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal lineWithTax = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return lineWithTax.subtract(lineBase);
                }, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
        ));
    }
}