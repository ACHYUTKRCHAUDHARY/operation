package com.achyut.operation.support;

import com.achyut.operation.asset.Asset;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="complaints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Complaint {
    public enum Status { OPEN, IN_REVIEW, ASSIGNED, RESOLVED, REJECTED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) private String complaintNumber;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) private Asset asset;
    @Column(nullable=false,length=2000) private String description;
    private String photoUrl;
    @Column(nullable=false) private boolean warrantyCovered;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private Status status=Status.OPEN;
    private String assignedTo;
    private String resolutionNote;
    private Instant createdAt;
    private Instant resolvedAt;
    @PrePersist void prePersist(){ if(createdAt==null) createdAt=Instant.now(); }
}
