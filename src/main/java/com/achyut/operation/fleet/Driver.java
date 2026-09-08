package com.achyut.operation.fleet;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name="drivers", indexes={@Index(name="idx_driver_phone", columnList="phone", unique=true)})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Driver {
    public enum DriverStatus { AVAILABLE, ON_DELIVERY, OFF_DUTY, INACTIVE }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String name;
    @Column(nullable=false, unique=true) private String phone;
    @Column(nullable=false, unique=true) private String licenseNumber;
    private LocalDate licenseExpiry;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private DriverStatus status=DriverStatus.AVAILABLE;
}
