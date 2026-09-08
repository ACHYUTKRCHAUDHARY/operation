package com.achyut.operation.work;

import com.achyut.operation.asset.Asset;
import com.achyut.operation.customer.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "work_orders", indexes = @Index(name = "idx_work_order_number", columnList = "orderNumber", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkOrder {
    public enum WorkType { CONTAINER_REPAIR, PORTA_CABIN_REPAIR, PORTA_CABIN_PRODUCTION, INSTALLATION, MAINTENANCE }
    public enum WorkStatus { CREATED, INSPECTION_PENDING, ESTIMATE_PENDING, CUSTOMER_APPROVAL_PENDING, APPROVED, IN_PROGRESS, BLOCKED, QUALITY_CHECK, READY_FOR_DISPATCH, COMPLETED, CANCELLED }
    public enum Priority { NORMAL, HIGH, URGENT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Customer customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Asset asset;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private WorkType workType;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private WorkStatus status;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Priority priority;

    @Column(length = 2000)
    private String scopeOfWork;
    private String assignedTeam;
    private String blockedReason;
    private Integer progressPercent;
    private BigDecimal estimatedCost;
    private BigDecimal approvedCost;
    private Instant expectedCompletionAt;
    private Instant actualCompletionAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now(); updatedAt = createdAt;
        if (status == null) status = WorkStatus.CREATED;
        if (priority == null) priority = Priority.NORMAL;
        if (progressPercent == null) progressPercent = 0;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
