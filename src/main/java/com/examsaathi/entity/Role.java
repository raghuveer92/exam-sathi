package com.examsaathi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity for RBAC (ROLE_STUDENT, ROLE_ADMIN).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleName name;

    public enum RoleName {
        ROLE_STUDENT,
        ROLE_ADMIN
    }
}
