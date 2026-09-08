package com.achyut.operation.procurement;

import com.achyut.operation.inventory.InventoryItem;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="purchase_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseRequest {
    public enum Status { REQUESTED, APPROVED, ORDERED, RECEIVED, REJECTED, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String requestNumber;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) private InventoryItem inventoryItem;
    @ManyToOne(fetch=FetchType.LAZY) private Supplier supplier;
    @Column(nullable=false, precision=14, scale=3) private BigDecimal quantity;
    @Column(precision=14, scale=2) private BigDecimal expectedUnitCost;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private Status status=Status.REQUESTED;
    private String requestedBy;
    private String approvedBy;
    private String note;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant receivedAt;
    @PrePersist void prePersist(){ if(requestedAt==null) requestedAt=Instant.now(); }
}
