package org.example.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE products SET is_deleted = true WHERE id=?")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "sale_until")
    private LocalDateTime saleUntil;

    @Column(nullable = false)
    private double stockQuantity;

    @Column(name = "min_stock_level", nullable = false)
    @Builder.Default
    private double minStockLevel = 0.0;

    private String imageUrl;

    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    @Column(nullable = false)
    private String unit;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeItem> recipe = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "tax_rate_id")
    private TaxRate taxRate;

    public BigDecimal getActiveBasePrice() {
        if (salePrice != null) {
            if (saleUntil == null || LocalDateTime.now().isBefore(saleUntil)) {
                return salePrice;
            }
        }
        return price;
    }


    public BigDecimal getPriceWithTax() {
        BigDecimal base = getActiveBasePrice();
        return getBigDecimal(base);
    }


    public BigDecimal getRegularPriceWithTax() {
        return getBigDecimal(price);
    }

    @NonNull
    private BigDecimal getBigDecimal(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        if (taxRate == null || taxRate.getRate() == null) return price.setScale(0, RoundingMode.HALF_UP);

        BigDecimal multiplier = taxRate.getRate().divide(new BigDecimal("100")).add(BigDecimal.ONE);
        return price.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
    }

    public boolean isOnSale() {
        return salePrice != null && (saleUntil == null || LocalDateTime.now().isBefore(saleUntil));
    }

    public String getDisplayImageUrl() {
        if (imageUrl == null || imageUrl.isBlank()) return "/images/placeholder.png";
        return "/images/" + imageUrl;
    }

    public enum ProductType {
        MATERIAL, PRODUCT
    }
    @Column(name = "available_lazures")
    private String availableLazures;

    @Column(name = "available_roof_colors")
    private String availableRoofColors;

    @Column(name = "available_designs")
    private String availableDesigns;
}