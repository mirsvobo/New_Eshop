package org.example.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User performedBy;

    @Column(nullable = false)
    private double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    @Getter
    @RequiredArgsConstructor
    public enum MovementType {
        RECEIPT("Příjemka"),
        SALE("Prodej e-shop"),
        ISSUE("Ostatní výdej"),
        PRODUCTION_IN("Příjem z výroby"),
        PRODUCTION_OUT("Výdej do výroby"),
        ADJUSTMENT_PLUS("Korekce plus (inventura)"),
        ADJUSTMENT_MINUS("Korekce mínus (inventura)");

        private final String displayName;
    }
}