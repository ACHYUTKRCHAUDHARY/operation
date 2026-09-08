package com.achyut.operation.procurement;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="suppliers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    @Builder.Default @Column(nullable=false) private boolean active=true;
}
