package com.achyut.operation.commercial;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private Invoice invoice;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal amount;
    @Column(nullable=false) private String method;
    private String referenceNumber;
    private String note;
    private Instant paidAt;
    @PrePersist void prePersist(){ if(paidAt==null) paidAt=Instant.now(); }
}
