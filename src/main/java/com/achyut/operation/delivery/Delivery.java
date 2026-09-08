package com.achyut.operation.delivery;

import com.achyut.operation.asset.Asset;
import com.achyut.operation.work.WorkOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries", indexes = @Index(name = "idx_delivery_public_token", columnList = "public_tracking_token", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Delivery {
    public enum DeliveryStatus { PLANNED, VEHICLE_ASSIGNED, DISPATCHED, IN_TRANSIT, DELAYED, NEAR_DESTINATION, DELIVERED, INSTALLATION_IN_PROGRESS, INSTALLED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String deliveryNumber;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private WorkOrder workOrder;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Asset asset;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private DeliveryStatus status;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private String vehicleType;
    private Double destinationLatitude;
    private Double destinationLongitude;
    @Column(length = 1200)
    private String destinationAddress;
    private Instant expectedDeliveryAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private String proofOfDeliveryUrl;
    private String receivedBy;
    @Column(name = "public_tracking_token", nullable = false, unique = true, length = 64)
    private String publicTrackingToken;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void prePersist() {
        createdAt = Instant.now(); updatedAt = createdAt;
        if (status == null) status = DeliveryStatus.PLANNED;
        if (publicTrackingToken == null || publicTrackingToken.isBlank()) publicTrackingToken = UUID.randomUUID().toString().replace("-", "");
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
