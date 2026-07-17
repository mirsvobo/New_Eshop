package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal basePrice;
    private BigDecimal originalPrice;
    private BigDecimal taxRateValue;
    private String selectedLazure;
    private String selectedRoofColor;
    private String selectedDesign;

    // NOVÉ: Aktuální stav skladu v momentě vložení/kontroly
    private double stockQuantity;

    // NOVÉ: Pomocná metoda pro vyhodnocení nutnosti výroby
    public boolean isRequiresManufacturing() {
        return this.quantity > this.stockQuantity;
    }
}