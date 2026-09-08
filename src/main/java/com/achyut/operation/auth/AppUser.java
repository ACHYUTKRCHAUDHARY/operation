package com.achyut.operation.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_users", indexes = @Index(name = "idx_app_users_email", columnList = "email", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;
    @Builder.Default @Column(nullable = false)
    private boolean enabled = true;

    public enum Role { ADMIN, OPERATIONS_MANAGER, WORKSHOP_MANAGER, QC_INSPECTOR, DRIVER, CUSTOMER }
}
