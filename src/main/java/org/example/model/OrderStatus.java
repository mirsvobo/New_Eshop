package org.example.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String colorClass;

    @Builder.Default
    private boolean active = true;

    private int displayOrder;
    public String getBadgeClass() {
        return colorClass + " px-3 py-1 inline-flex text-[10px] leading-5 font-black rounded-full uppercase tracking-widest border border-current/10";
    }
}