package com.achyut.operation.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, unique = true)
    private String sku;

    @NotBlank @Column(nullable = false)
    private String name;

    private String unit;
    private BigDecimal quantityOnHand;
    private BigDecimal reorderLevel;
    private BigDecimal unitCost;
    private String preferredSupplier;
    private Instant updatedAt;

    @PrePersist @PreUpdate
    void touch() {
        updatedAt = Instant.now();
        if (quantityOnHand == null) quantityOnHand = BigDecimal.ZERO;
        if (reorderLevel == null) reorderLevel = BigDecimal.ZERO;
    }

    @Transient
    public boolean isLowStock() {
        return quantityOnHand != null && reorderLevel != null && quantityOnHand.compareTo(reorderLevel) <= 0;
    }
}
