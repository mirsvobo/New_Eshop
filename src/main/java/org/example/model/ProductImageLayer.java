package org.example.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
@Entity
@Check(
        name = "chk_product_image_layer_file",
        constraints = "(((type = 'LAZURE' AND lower(option_name) = 'afromorsia') OR (type = 'ROOF_COLOR' AND lower(option_name) = 'antracit')) AND image_url IS NULL AND active = true AND sort_order = -1000) OR ((type <> 'LAZURE' OR lower(option_name) <> 'afromorsia') AND (type <> 'ROOF_COLOR' OR lower(option_name) <> 'antracit') AND image_url IS NOT NULL AND char_length(trim(image_url)) > 0 AND sort_order >= 0)"
)
@Table(
        name = "product_image_layers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_image_layer_option",
                columnNames = {"product_id", "type", "option_name"}
        ),
        indexes = {
                @Index(
                        name = "idx_product_image_layer_product_type_active",
                        columnList = "product_id,type,active"
                ),
                @Index(
                        name = "idx_product_image_layer_product_sort",
                        columnList = "product_id,type,sort_order"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductImageLayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LayerType type;
    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;
    @Column(name = "image_url", length = 255)
    private String imageUrl;
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    public String getDisplayImageUrl() {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return "/images/" + imageUrl;
    }
    public boolean isDefaultOption() {
        return type != null && type.isDefaultOption(optionName);
    }
}
