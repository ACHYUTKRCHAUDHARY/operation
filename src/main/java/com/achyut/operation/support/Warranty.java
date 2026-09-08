package com.achyut.operation.support;

import com.achyut.operation.asset.Asset;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name="warranties")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Warranty {
    public enum Status { ACTIVE, EXPIRED, VOID }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) private Asset asset;
    @Column(nullable=false) private LocalDate startDate;
    @Column(nullable=false) private LocalDate endDate;
    @Column(length=1500) private String coverageDetails;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private Status status=Status.ACTIVE;
}
