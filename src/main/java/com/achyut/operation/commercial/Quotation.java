package com.achyut.operation.commercial;

import com.achyut.operation.work.WorkOrder;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="quotations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quotation {
    public enum Status { DRAFT, SENT, APPROVED, REJECTED, EXPIRED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String quotationNumber;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private WorkOrder workOrder;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal baseAmount;
    @Column(nullable=false, precision=14, scale=2) @Builder.Default private BigDecimal transportCharge=BigDecimal.ZERO;
    @Column(nullable=false, precision=14, scale=2) @Builder.Default private BigDecimal taxAmount=BigDecimal.ZERO;
    @Column(nullable=false, precision=14, scale=2) @Builder.Default private BigDecimal discount=BigDecimal.ZERO;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private Status status=Status.DRAFT;
    private LocalDate validUntil;
    private Instant approvedAt;
    private Instant createdAt;
    @PrePersist void prePersist(){ if(createdAt==null) createdAt=Instant.now(); }
}
