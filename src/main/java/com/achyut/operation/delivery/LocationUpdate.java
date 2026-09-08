package com.achyut.operation.delivery;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "location_updates", indexes = @Index(name = "idx_location_delivery_time", columnList = "delivery_id,recordedAt"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocationUpdate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Delivery delivery;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double accuracyMeters;
    private Double speedKph;
    private Instant recordedAt;

    @PrePersist void prePersist() { if (recordedAt == null) recordedAt = Instant.now(); }
}
