package com.achyut.operation.fleet;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name="vehicles", indexes={@Index(name="idx_vehicle_registration", columnList="registrationNumber", unique=true)})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle {
    public enum VehicleStatus { AVAILABLE, ASSIGNED, IN_TRANSIT, MAINTENANCE, INACTIVE }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String registrationNumber;
    @Column(nullable=false) private String vehicleType;
    private String capacityDescription;
    private LocalDate insuranceExpiry;
    private LocalDate permitExpiry;
    private LocalDate serviceDueAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private VehicleStatus status=VehicleStatus.AVAILABLE;
}
