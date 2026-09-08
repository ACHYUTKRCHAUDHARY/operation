package com.achyut.operation.work;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "work_updates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkUpdate {
    public enum Stage { INSPECTION, WELDING, FLOORING, FABRICATION, ELECTRICAL, PLUMBING, INTERIOR, PAINTING, CLEANING, QUALITY_CHECK, INSTALLATION, OTHER }
    public enum StageStatus { PENDING, IN_PROGRESS, COMPLETED, BLOCKED, VERIFIED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Stage stage;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StageStatus status;

    private String assignedTo;
    private String photoUrl;

    @Column(length = 2000)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
