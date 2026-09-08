package com.achyut.operation.asset;

import com.achyut.operation.customer.Customer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "assets", indexes = @Index(name = "idx_asset_code", columnList = "assetCode", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asset {
    public enum AssetType { CONTAINER, PORTA_CABIN }
    public enum AssetStatus { RECEIVED, INSPECTION, REPAIR_IN_PROGRESS, PRODUCTION_IN_PROGRESS, QUALITY_CHECK, READY_FOR_DISPATCH, DISPATCHED, IN_TRANSIT, DELIVERED, INSTALLED, ON_HOLD }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String assetCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    private String sizeDescription;
    private String serialNumber;
    private String currentYardLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now(); updatedAt = createdAt;
        if (status == null) status = AssetStatus.RECEIVED;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
