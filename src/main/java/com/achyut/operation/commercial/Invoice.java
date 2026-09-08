package com.achyut.operation.commercial;

import com.achyut.operation.work.WorkOrder;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {
    public enum Status { DRAFT, ISSUED, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String invoiceNumber;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private WorkOrder workOrder;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal amountDue;
    @Column(nullable=false, precision=14, scale=2) @Builder.Default private BigDecimal amountPaid=BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private Status status=Status.DRAFT;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Instant createdAt;
    @PrePersist void prePersist(){ if(createdAt==null) createdAt=Instant.now(); if(issueDate==null) issueDate=LocalDate.now(); }
}
